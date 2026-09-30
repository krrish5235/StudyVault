package com.studyvault.reviews;

import com.studyvault.common.api.ApiResponse;
import com.studyvault.common.api.PageResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/reviews")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminReviewController {

    private final ReviewRepository reviews;
    private final ReviewService reviewService;

    public record AdminReviewDto(UUID id, String productTitle, String userEmail, int rating,
                                 String comment, String status, Instant createdAt) {
    }

    @GetMapping
    public ApiResponse<PageResponse<AdminReviewDto>> list(@RequestParam(defaultValue = "0") int page,
                                                          @RequestParam(defaultValue = "20") int size) {
        var result = reviews.findAllByOrderByCreatedAtDesc(
                PageRequest.of(Math.max(0, page), Math.min(50, Math.max(1, size))));
        var mapped = result.map(r -> new AdminReviewDto(r.getId(), r.getProduct().getTitle(),
                r.getUser().getEmail(), r.getRating(), r.getComment(), r.getStatus().name(), r.getCreatedAt()));
        return ApiResponse.ok(PageResponse.of(mapped));
    }

    @PostMapping("/{id}/hide")
    public ApiResponse<Void> hide(@PathVariable UUID id, HttpServletRequest httpRequest) {
        reviewService.moderate(id, Review.Status.HIDDEN, httpRequest);
        return ApiResponse.message("Review hidden.");
    }

    @PostMapping("/{id}/publish")
    public ApiResponse<Void> publish(@PathVariable UUID id, HttpServletRequest httpRequest) {
        reviewService.moderate(id, Review.Status.PUBLISHED, httpRequest);
        return ApiResponse.message("Review published.");
    }
}
