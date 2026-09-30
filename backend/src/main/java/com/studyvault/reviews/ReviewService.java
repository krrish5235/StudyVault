package com.studyvault.reviews;

import com.studyvault.catalog.Product;
import com.studyvault.catalog.ProductRepository;
import com.studyvault.common.audit.AuditService;
import com.studyvault.common.error.BadRequestException;
import com.studyvault.common.error.ConflictException;
import com.studyvault.common.error.ForbiddenException;
import com.studyvault.common.error.NotFoundException;
import com.studyvault.orders.EntitlementRepository;
import com.studyvault.security.SecurityUtils;
import com.studyvault.security.SecurityUser;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Only verified purchasers may review; one review per user per product; product
 * rating aggregates are recomputed in the same transaction as any change.
 */
@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviews;
    private final ProductRepository products;
    private final EntitlementRepository entitlements;
    private final com.studyvault.user.UserRepository users;
    private final AuditService audit;

    public record ReviewDto(UUID id, UUID productId, String userFullName, int rating,
                            String comment, String status, Instant createdAt) {

        public static ReviewDto from(Review r) {
            return new ReviewDto(r.getId(), r.getProduct().getId(), r.getUser().getFullName(),
                    r.getRating(), r.getComment(), r.getStatus().name(), r.getCreatedAt());
        }
    }

    @Transactional(readOnly = true)
    public Page<ReviewDto> listForProduct(UUID productId, int page, int size) {
        Page<Review> result = reviews.findAllByProductIdAndStatusOrderByCreatedAtDesc(
                productId, Review.Status.PUBLISHED, PageRequest.of(Math.max(0, page), Math.min(20, Math.max(1, size))));
        return result.map(ReviewDto::from);
    }

    @Transactional
    public ReviewDto create(UUID productId, int rating, String comment, HttpServletRequest httpRequest) {
        SecurityUser su = SecurityUtils.requireUser();
        if (!entitlements.existsByUserIdAndProductIdAndRevokedAtIsNull(su.id(), productId)) {
            throw new ForbiddenException("Only verified purchasers can review this product.");
        }
        if (reviews.findByProductIdAndUserId(productId, su.id()).isPresent()) {
            throw new ConflictException("You have already reviewed this product.");
        }

        Product product = products.findById(productId)
                .orElseThrow(() -> new NotFoundException("Product not found."));
        Review review = new Review();
        review.setProduct(product);
        review.setUser(users.getReferenceById(su.id()));
        review.setRating(rating);
        review.setComment(comment == null ? null : comment.trim());
        reviews.saveAndFlush(review);

        recomputeAggregates(product);
        audit.record(httpRequest, "REVIEW_CREATED", "product", productId.toString(), Map.of("rating", rating));
        return ReviewDto.from(review);
    }

    @Transactional
    public ReviewDto update(UUID productId, int rating, String comment, HttpServletRequest httpRequest) {
        SecurityUser su = SecurityUtils.requireUser();
        Review review = reviews.findByProductIdAndUserId(productId, su.id())
                .orElseThrow(() -> new NotFoundException("Review not found."));
        review.setRating(rating);
        review.setComment(comment == null ? null : comment.trim());
        reviews.saveAndFlush(review);
        recomputeAggregates(review.getProduct());
        audit.record(httpRequest, "REVIEW_UPDATED", "product", productId.toString(), Map.of("rating", rating));
        return ReviewDto.from(review);
    }

    @Transactional
    public void moderate(UUID reviewId, Review.Status status, HttpServletRequest httpRequest) {
        Review review = reviews.findById(reviewId).orElseThrow(() -> new NotFoundException("Review not found."));
        review.setStatus(status);
        reviews.saveAndFlush(review);
        recomputeAggregates(review.getProduct());
        audit.record(httpRequest, status == Review.Status.HIDDEN ? "REVIEW_HIDDEN" : "REVIEW_PUBLISHED",
                "review", reviewId.toString(), Map.of());
    }

    private void recomputeAggregates(Product product) {
        double avg = reviews.averageRating(product.getId());
        long count = reviews.countByProductIdAndStatus(product.getId(), Review.Status.PUBLISHED);
        product.setAvgRating(BigDecimal.valueOf(avg).setScale(2, RoundingMode.HALF_UP));
        product.setReviewCount((int) count);
        products.save(product);
    }
}
