package com.studyvault.catalog;

import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;

/** Composable JPA criteria for the public catalog search. All values are bound parameters. */
public final class ProductSpecifications {

    private ProductSpecifications() {
    }

    public static Specification<Product> hasStatus(Product.Status status) {
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    public static Specification<Product> inCategory(String categorySlug) {
        return (root, query, cb) -> cb.equal(root.join("category").get("slug"), categorySlug);
    }

    public static Specification<Product> hasTag(String tagSlug) {
        return (root, query, cb) -> cb.equal(root.join("tags").get("slug"), tagSlug);
    }

    public static Specification<Product> matchesQuery(String q) {
        String like = "%" + q.toLowerCase() + "%";
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("title")), like),
                cb.like(cb.lower(root.get("shortDescription")), like));
    }

    public static Specification<Product> priceMin(Long minMinor) {
        return (root, query, cb) -> cb.ge(root.get("priceMinor"), minMinor);
    }

    public static Specification<Product> priceMax(Long maxMinor) {
        return (root, query, cb) -> cb.le(root.get("priceMinor"), maxMinor);
    }

    public static Specification<Product> ratingAtLeast(BigDecimal rating) {
        return (root, query, cb) -> cb.ge(root.get("avgRating"), rating);
    }
}
