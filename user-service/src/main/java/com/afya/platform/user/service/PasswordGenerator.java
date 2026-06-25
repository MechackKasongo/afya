package com.afya.platform.user.service;

import java.security.SecureRandom;
import java.text.Normalizer;
import java.util.Locale;

public final class PasswordGenerator {

    private static final String UPPER = "ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final String LOWER = "abcdefghjkmnpqrstuvwxyz";
    private static final String DIGITS = "23456789";
    private static final String SPECIAL = "@#$%&*+-";

    private PasswordGenerator() {
    }

    public static String suggest(String firstName, String lastName, String postName, int length, int variation) {
        int safeLength = length <= 0 ? 16 : Math.min(Math.max(length, 12), 24);
        String seed = slug(firstName) + "." + slug(lastName) + "." + slug(postName) + "." + variation;
        SecureRandom random = new SecureRandom(seed.getBytes());
        StringBuilder password = new StringBuilder(safeLength);
        password.append(pick(random, UPPER));
        password.append(pick(random, LOWER));
        password.append(pick(random, DIGITS));
        password.append(pick(random, SPECIAL));
        String all = UPPER + LOWER + DIGITS + SPECIAL;
        while (password.length() < safeLength) {
            password.append(pick(random, all));
        }
        return password.toString();
    }

    private static char pick(SecureRandom random, String alphabet) {
        return alphabet.charAt(random.nextInt(alphabet.length()));
    }

    private static String slug(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String normalized = Normalizer.normalize(value.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "");
        return normalized.isEmpty() ? "x" : normalized;
    }
}
