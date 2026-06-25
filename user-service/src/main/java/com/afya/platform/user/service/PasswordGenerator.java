package com.afya.platform.user.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.Normalizer;
import java.util.Locale;
import java.util.Random;

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
        Random random = seededRandom(seed);
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

    private static Random seededRandom(String seed) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(seed.getBytes(StandardCharsets.UTF_8));
            long seedLong = 0;
            for (int i = 0; i < 8; i++) {
                seedLong = (seedLong << 8) | (hash[i] & 0xffL);
            }
            return new Random(seedLong);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 unavailable", ex);
        }
    }

    private static char pick(Random random, String alphabet) {
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
