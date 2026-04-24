package com.kangworld.fantasybaseball.point.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class BaseballInnings {

    public static int toOuts(String inn) {
        if (inn == null || inn.isBlank())
            return 0;

        String normalized = inn.trim();
        String[] parts = normalized.split("\\s+");
        int innings = parseInt(parts[0]);
        int outs = innings * 3;

        if (parts.length < 2)
            return outs;

        return outs + switch (parts[1]) {
            case "⅓", "1/3" -> 1;
            case "⅔", "2/3" -> 2;
            default -> 0;
        };
    }

    private static int parseInt(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

}
