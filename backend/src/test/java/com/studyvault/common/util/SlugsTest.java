package com.studyvault.common.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SlugsTest {

    @Test
    void slugifiesTitles() {
        assertThat(Slugs.slugify("Spring Boot Complete Notes")).isEqualTo("spring-boot-complete-notes");
        assertThat(Slugs.slugify("  Java & SQL  ")).isEqualTo("java-sql");
        assertThat(Slugs.slugify("C/C++ Notes")).isEqualTo("c-c-notes");
        assertThat(Slugs.slugify("«Ünïcodé»")).isEqualTo("n-cod");
    }

    @Test
    void blankInputYieldsFallback() {
        assertThat(Slugs.slugify("!!!")).isEqualTo("item");
        assertThat(Slugs.slugify(null)).isEqualTo("item");
    }
}
