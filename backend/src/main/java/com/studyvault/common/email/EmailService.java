package com.studyvault.common.email;

import com.studyvault.config.AppProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import java.util.List;

/**
 * Transactional email composition. All dynamic values are HTML-escaped. Sending is
 * asynchronous with a dedicated executor - email failures must never break business
 * transactions.
 */
@Service
@RequiredArgsConstructor
public class EmailService {

    private final EmailSender sender;
    private final AppProperties props;

    @Async("emailExecutor")
    public void sendVerification(String to, String fullName, String rawToken) {
        String link = props.getFrontendUrl() + "/verify-email?token=" + rawToken;
        send(to, "Verify your email - StudyVault", """
                <p>Hi %s,</p>
                <p>Welcome to StudyVault! Please confirm your email address:</p>
                <p><a href="%s">Verify my email</a></p>
                <p>This link expires in 24 hours. If you did not create an account, you can ignore this email.</p>
                """.formatted(esc(fullName), link));
    }

    @Async("emailExecutor")
    public void sendWelcome(String to, String fullName) {
        send(to, "Welcome to StudyVault", """
                <p>Hi %s,</p>
                <p>Your StudyVault account is ready. Happy learning!</p>
                """.formatted(esc(fullName)));
    }

    @Async("emailExecutor")
    public void sendPasswordReset(String to, String rawToken) {
        String link = props.getFrontendUrl() + "/reset-password?token=" + rawToken;
        send(to, "Reset your password - StudyVault", """
                <p>We received a request to reset your StudyVault password.</p>
                <p><a href="%s">Choose a new password</a></p>
                <p>This link expires in 30 minutes and can be used once. All sessions will be signed out after a reset.</p>
                """.formatted(link));
    }

    @Async("emailExecutor")
    public void sendPasswordChanged(String to, String fullName) {
        send(to, "Your password was changed - StudyVault", """
                <p>Hi %s,</p>
                <p>Your StudyVault password was just changed. If this was not you, reset your password immediately.</p>
                """.formatted(esc(fullName)));
    }

    @Async("emailExecutor")
    public void sendAccountLocked(String to) {
        send(to, "Security alert - StudyVault", """
                <p>Multiple failed sign-in attempts were detected on your account, so it has been temporarily locked for 15 minutes.</p>
                <p>If this was not you, consider changing your password.</p>
                """);
    }

    @Async("emailExecutor")
    public void sendOrderConfirmation(String to, String fullName, String orderNumber, long totalMinor,
                                      List<OrderLine> lines) {
        StringBuilder rows = new StringBuilder();
        for (OrderLine line : lines) {
            rows.append("<tr><td>").append(esc(line.title()))
                    .append("</td><td style='text-align:right'>₹").append(formatMinor(line.amountMinor()))
                    .append("</td></tr>");
        }
        String body = "<p>Hi " + esc(fullName) + ",</p>"
                + "<p>Your payment was successful. Your order <b>" + esc(orderNumber)
                + "</b> is confirmed and the products are now available in your library.</p>"
                + "<table cellpadding=\"6\">" + rows + "</table>"
                + "<p><b>Total paid: ₹" + formatMinor(totalMinor) + "</b></p>";
        send(to, "Order confirmed - StudyVault", body);
    }

    public record OrderLine(String title, long amountMinor) {
    }

    private void send(String to, String subject, String body) {
        sender.send(to, subject, wrap(body));
    }

    private String wrap(String body) {
        return """
                <!DOCTYPE html><html><body style="font-family:Arial,sans-serif;color:#1a202c;max-width:560px">
                <div style="padding:24px;background:#f8fafc;border-radius:8px">%s</div>
                <p style="font-size:12px;color:#64748b">StudyVault - you are receiving this because you have a StudyVault account.</p>
                </body></html>""".formatted(body);
    }

    private String esc(String value) {
        return HtmlUtils.htmlEscape(value == null ? "" : value);
    }

    private String formatMinor(long minor) {
        return java.math.BigDecimal.valueOf(minor, 2).toPlainString();
    }
}
