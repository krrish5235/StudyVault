package com.studyvault.reviews;

import com.studyvault.common.api.ApiResponse;
import com.studyvault.common.api.PageResponse;
import com.studyvault.common.api.PageResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    public record UpsertReviewRequest(@NotNull @Min(1) @Max(5) Integer rating,
                                      @Size(max = 2000) String comment) {
    }

    @GetMapping("/products/{id}/reviews")
    public ApiResponse<PageResponse<ReviewService.ReviewDto>> list(@PathVariable UUID id,
                                                                   @RequestParam(defaultValue = "0") int page,
                                                                   @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(PageResponse.of(reviewService.listForProduct(id, page, size)));
    }

    @PostMapping("/products/{id}/reviews")
    public ApiResponse<ReviewService.ReviewDto> create(@PathVariable UUID id,
                                                       @Valid @RequestBody UpsertReviewRequest request,
                                                       HttpServletRequest httpRequest) {
        return ApiResponse.ok(reviewService.create(id, request.rating(), request.comment(), httpRequest),
                "Review posted. Thank you!");
    }

    @PutMapping("/products/{id}/reviews")
    public ApiResponse<ReviewService.ReviewDto> update(@PathVariable UUID id,
                                                       @Valid @RequestBody UpsertReviewRequest request,
                                                       HttpServletRequest httpRequest) {
        return ApiResponse.ok(reviewService.update(id, request.rating(), request.comment(), httpRequest),
                "Review updated.");
    }
}
