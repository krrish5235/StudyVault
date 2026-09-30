package com.studyvault.admin;

import com.studyvault.catalog.CategoryService;
import com.studyvault.catalog.Product;
import com.studyvault.catalog.ProductRepository;
import com.studyvault.common.audit.AuditService;
import com.studyvault.common.error.BadRequestException;
import com.studyvault.common.error.NotFoundException;
import com.studyvault.orders.Order;
import com.studyvault.orders.OrderRepository;
import com.studyvault.user.UserRepository;
import com.studyvault.config.AppProperties;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Admin Command Center: natural-language commands mapped ONLY to whitelisted
 * backend actions. No generated SQL or shell commands, ever. Destructive or
 * money-affecting actions return a signed confirmation plan that must be
 * confirmed explicitly before execution.
 */
@RestController
@RequestMapping("/api/v1/admin/command")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class CommandCenterController {

    private final ProductRepository products;
    private final UserRepository users;
    private final OrderRepository orders;
    private final CategoryService categoryService;
    private final AdminDashboardController dashboard;
    private final AuditService audit;
    private final AppProperties props;

    private final SecureRandom random = new SecureRandom();
    /** Per-boot signing secret for confirmation tokens - tokens never survive a restart. */
    private final byte[] confirmSecret = new byte[32];
    {
        random.nextBytes(confirmSecret);
    }
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();

    public record CommandRequest(@org.springframework.lang.NonNull String text) {
    }

    public record CommandResponse(String kind, String summary, Object data, String confirmToken) {
    }

    @PostMapping
    @Transactional(readOnly = true)
    public CommandResponse execute(@RequestBody CommandRequest request) {
        String text = request.text() == null ? "" : request.text().trim();
        if (text.isEmpty()) {
            throw new BadRequestException("Type a command, e.g. \"show today's orders\" or \"find user alice@example.com\".");
        }
        String lower = text.toLowerCase();

        // ---- read-only commands (execute immediately) ----
        if (lower.matches(".*(show|list).*(revenue|sales).*") || lower.matches(".*how (much|many) (revenue|sales).*")) {
            var data = dashboard.dashboard().data();
            return new CommandResponse("data", "Revenue overview.", data, null);
        }
        if (lower.matches(".*(show|list).*(orders?).*")) {
            List<Map<String, Object>> result = orders.findAllByOrderByCreatedAtDesc(
                            org.springframework.data.domain.PageRequest.of(0, 10)).stream()
                    .map(o -> Map.<String, Object>of(
                            "orderNumber", o.getOrderNumber(),
                            "status", o.getStatus().name(),
                            "totalMinor", o.getTotalMinor(),
                            "created", o.getCreatedAt().toString()))
                    .toList();
            return new CommandResponse("data", "Latest 10 orders.", result, null);
        }
        Matcher findUser = Pattern.compile("find user\\s+(\\S+@\\S+)").matcher(lower);
        if (findUser.find()) {
            var user = users.findByEmail(findUser.group(1).toLowerCase())
                    .orElseThrow(() -> new NotFoundException("No user with that email."));
            return new CommandResponse("data", "User found.", Map.of(
                    "email", user.getEmail(), "name", user.getFullName(),
                    "verified", user.isEmailVerified(), "enabled", user.isEnabled()), null);
        }
        Matcher price = Pattern.compile("change (?:the )?price of (.+?) to (\\d+)").matcher(lower);
        if (price.find()) {
            Product product = findProduct(price.group(1));
            long newPriceMinor = Long.parseLong(price.group(2)) * 100;
            if (newPriceMinor <= 0) {
                throw new BadRequestException("Price must be greater than zero.");
            }
            Map<String, Object> params = Map.of("productId", product.getId().toString(), "priceMinor", newPriceMinor);
            return new CommandResponse("confirm_required",
                    String.format("Change \"%s\" price from ₹%s to ₹%s?",
                            product.getTitle(), product.getPriceMinor() / 100, newPriceMinor / 100),
                    params, sign("SET_PRICE", params));
        }
        Matcher createCategory = Pattern.compile("create category (.+)").matcher(lower);
        if (createCategory.find()) {
            Map<String, Object> params = Map.of("name", createCategory.group(1).trim());
            return new CommandResponse("confirm_required",
                    "Create category \"" + params.get("name") + "\"?", params, sign("CREATE_CATEGORY", params));
        }
        Matcher publish = Pattern.compile("(publish|unpublish) (.+)").matcher(lower);
        if (publish.find()) {
            Product product = findProduct(publish.group(2));
            Map<String, Object> params = Map.of("productId", product.getId().toString(), "action", publish.group(1));
            return new CommandResponse("confirm_required",
                    capitalize(publish.group(1)) + " \"" + product.getTitle() + "\"?", params, sign("SET_STATUS", params));
        }
        Matcher deleteProduct = Pattern.compile("(delete|archive) (?:the )?product (.+)").matcher(lower);
        if (deleteProduct.find()) {
            Product product = findProduct(deleteProduct.group(2));
            Map<String, Object> params = Map.of("productId", product.getId().toString(), "action", "archive");
            return new CommandResponse("confirm_required",
                    "Archive \"" + product.getTitle() + "\"? It will be hidden from the store.", params,
                    sign("ARCHIVE_PRODUCT", params));
        }

        throw new BadRequestException("Command not recognized. Try: \"show revenue\", \"show today's orders\", "
                + "\"find user <email>\", \"change price of <product> to <amount>\", \"publish <product>\", \"archive product <product>\".");
    }

    @PostMapping("/confirm")
    @Transactional
    public CommandResponse confirm(@RequestBody Map<String, String> body, HttpServletRequest httpRequest) {
        String token = body.get("confirmToken");
        if (token == null || token.isBlank()) {
            throw new BadRequestException("Missing confirmation token.");
        }
        SignedAction action = verify(token);
        audit.record(httpRequest, "COMMAND_CENTER_EXECUTED", "command", action.type(),
                Map.of("params", action.params()));
        switch (action.type()) {
            case "SET_PRICE" -> {
                Product p = products.findById(UUID.fromString((String) action.params().get("productId")))
                        .orElseThrow(() -> new NotFoundException("Product not found."));
                p.setPriceMinor(((Number) action.params().get("priceMinor")).longValue());
                products.save(p);
                return new CommandResponse("done", "Price updated for \"" + p.getTitle() + "\".", null, null);
            }
            case "CREATE_CATEGORY" -> {
                String name = (String) action.params().get("name");
                if (name == null || name.isBlank() || name.length() > 100) {
                    throw new BadRequestException("Invalid category name.");
                }
                var created = categoryService.create(new com.studyvault.catalog.dto.CreateCategoryRequest(name, null, null), httpRequest);
                return new CommandResponse("done", "Category \"" + created.name() + "\" created.", created, null);
            }
            case "SET_STATUS" -> {
                Product p = products.findById(UUID.fromString((String) action.params().get("productId")))
                        .orElseThrow(() -> new NotFoundException("Product not found."));
                boolean publish = "publish".equalsIgnoreCase((String) action.params().get("action"));
                p.setStatus(publish ? Product.Status.PUBLISHED : Product.Status.DRAFT);
                products.save(p);
                return new CommandResponse("done", "\"" + p.getTitle() + "\" is now "
                        + (publish ? "published" : "a draft") + ".", null, null);
            }
            case "ARCHIVE_PRODUCT" -> {
                Product p = products.findById(UUID.fromString((String) action.params().get("productId")))
                        .orElseThrow(() -> new NotFoundException("Product not found."));
                p.setStatus(Product.Status.ARCHIVED);
                p.setFeatured(false);
                p.setBestseller(false);
                products.save(p);
                return new CommandResponse("done", "\"" + p.getTitle() + "\" archived.", null, null);
            }
            default -> throw new BadRequestException("Unknown action.");
        }
    }

    // ------------------------------------------------------------------ helpers

    private Product findProduct(String text) {
        String needle = text.replaceAll("[\"']", "").trim();
        return products.findAll().stream()
                .filter(p -> p.getTitle().equalsIgnoreCase(needle) || p.getSlug().equals(needle)
                        || p.getTitle().toLowerCase().contains(needle))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("No product matches \"" + needle + "\"."));
    }

    private String sign(String type, Map<String, Object> params) {
        try {
            String payload = type + "::" + objectMapper.writeValueAsString(params) + "::"
                    + (System.currentTimeMillis() + 300_000);
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(confirmSecret, "HmacSHA256"));
            String sigHex = java.util.HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
            String payloadB64 = Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(payload.getBytes(StandardCharsets.UTF_8));
            return payloadB64 + "." + sigHex;
        } catch (Exception e) {
            throw new IllegalStateException("Could not sign confirmation token", e);
        }
    }

    private SignedAction verify(String token) {
        try {
            int dot = token.lastIndexOf('.');
            if (dot <= 0) {
                throw new BadRequestException("Invalid confirmation token.");
            }
            String payload = new String(Base64.getUrlDecoder().decode(token.substring(0, dot)), StandardCharsets.UTF_8);
            String sigHex = token.substring(dot + 1);

            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(confirmSecret, "HmacSHA256"));
            byte[] expected = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            byte[] provided = java.util.HexFormat.of().parseHex(sigHex);
            if (!java.security.MessageDigest.isEqual(expected, provided)) {
                throw new BadRequestException("Invalid confirmation token.");
            }

            int first = payload.indexOf("::");
            int last = payload.lastIndexOf("::");
            if (first < 0 || last <= first) {
                throw new BadRequestException("Invalid confirmation token.");
            }
            String type = payload.substring(0, first);
            String paramsJson = payload.substring(first + 2, last);
            long expiry = Long.parseLong(payload.substring(last + 2));
            if (expiry < System.currentTimeMillis()) {
                throw new BadRequestException("Confirmation expired. Run the command again.");
            }
            Map<String, Object> params = objectMapper.readValue(paramsJson,
                    new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {
                    });
            return new SignedAction(type, params);
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            throw new BadRequestException("Invalid confirmation token.");
        }
    }

    private String capitalize(String s) {
        return s == null || s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private record SignedAction(String type, Map<String, Object> params) {
    }
}
