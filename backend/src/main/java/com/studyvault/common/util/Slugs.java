package com.studyvault.common.util;

/** URL slug generation with collation-safe output. */
public final class Slugs {

    private Slugs() {
    }

    public static String slugify(String input) {
        if (input == null) {
            return "item";
        }
        String slug = input.toLowerCase()
                .replace('&', ' ')
                .replace('/', ' ')
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("-{2,}", "-")
                .replaceAll("^-|-$", "");
        if (slug.isBlank()) {
            slug = "item";
        }
        return slug.length() > 200 ? slug.substring(0, 200) : slug;
    }
}
