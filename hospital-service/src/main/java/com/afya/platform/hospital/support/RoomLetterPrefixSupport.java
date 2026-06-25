package com.afya.platform.hospital.support;

import com.afya.platform.shared.exception.BadRequestException;

public final class RoomLetterPrefixSupport {

    private RoomLetterPrefixSupport() {
    }

    public static char resolve(String value, char defaultLetter) {
        if (value == null || value.isBlank()) {
            return defaultLetter;
        }
        String trimmed = value.strip();
        if (trimmed.length() != 1 || !Character.isLetter(trimmed.charAt(0))) {
            throw new BadRequestException("La lettre des chambres doit être une seule lettre (ex. A pour A1, A2)");
        }
        return Character.toUpperCase(trimmed.charAt(0));
    }
}
