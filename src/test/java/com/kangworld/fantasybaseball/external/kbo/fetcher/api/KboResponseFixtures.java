package com.kangworld.fantasybaseball.external.kbo.fetcher.api;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

import org.springframework.core.io.ClassPathResource;

final class KboResponseFixtures {

    private static final String ROOT = "kbo/";

    private KboResponseFixtures() {
    }

    static String json(String fileName) {
        return new String(bytes(fileName), StandardCharsets.UTF_8);
    }

    static byte[] bytes(String fileName) {
        try {
            return new ClassPathResource(ROOT + fileName).getInputStream().readAllBytes();
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

}
