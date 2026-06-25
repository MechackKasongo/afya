package com.afya.platform.hospital.support;

import java.text.Normalizer;

public final class DepartmentCodeGenerator {

    private static final int MAX_LENGTH = 40;

    private DepartmentCodeGenerator() {
    }

    /** Code technique dérivé du nom (lettres/chiffres, majuscules, sans accents). */
    public static String fromName(String name) {
        if (name == null || name.isBlank()) {
            return "DEPT";
        }
        String normalized = Normalizer.normalize(name.strip(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replaceAll("[^A-Za-z0-9]", "")
                .toUpperCase();
        if (normalized.isEmpty()) {
            return "DEPT";
        }
        return normalized.length() <= MAX_LENGTH ? normalized : normalized.substring(0, MAX_LENGTH);
    }

    /** Variante avec suffixe numérique en cas de collision (PEDIA, PEDIA2, …). */
    public static String withSuffix(String baseCode, int suffix) {
        if (suffix <= 1) {
            return baseCode;
        }
        String suffixPart = String.valueOf(suffix);
        int maxBase = MAX_LENGTH - suffixPart.length();
        String trimmed = baseCode.length() <= maxBase ? baseCode : baseCode.substring(0, maxBase);
        return trimmed + suffixPart;
    }
}
