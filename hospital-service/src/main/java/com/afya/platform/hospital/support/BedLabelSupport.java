package com.afya.platform.hospital.support;

public final class BedLabelSupport {

    private BedLabelSupport() {
    }

    /** Libellé catalogue unifié (ex. A1-01 = chambre A1, lit 01). */
    public static String toLabel(String room, String bed) {
        if (room == null || room.isBlank() || bed == null || bed.isBlank()) {
            return null;
        }
        return room.strip() + "-" + bed.strip();
    }

    public static Parsed parse(String label) {
        if (label == null || label.isBlank()) {
            return new Parsed(label, label);
        }
        int dash = label.indexOf('-');
        if (dash > 0) {
            return new Parsed(label.substring(0, dash), label.substring(dash + 1));
        }
        return new Parsed(label, label);
    }

    /** Chambre : lettre + numéro sans zéros initiaux (ex. A1, A12). */
    public static String formatRoomCode(char roomLetter, int roomSequence) {
        return Character.toUpperCase(roomLetter) + Integer.toString(roomSequence);
    }

    /** Clé de tri : lettre de chambre puis numéro (A1, A2, A10, B1…). */
    public static long roomOrderKey(String label) {
        String room = parse(label).room();
        if (room == null || room.isBlank()) {
            return Long.MAX_VALUE;
        }
        int digitStart = 0;
        while (digitStart < room.length() && !Character.isDigit(room.charAt(digitStart))) {
            digitStart++;
        }
        long letterKey = digitStart > 0 ? Character.toUpperCase(room.charAt(0)) - 'A' : 0;
        String digits = room.substring(digitStart).replaceAll("\\D", "");
        if (digits.isEmpty()) {
            return Long.MAX_VALUE;
        }
        try {
            return letterKey * 1_000_000L + Long.parseLong(digits);
        } catch (NumberFormatException e) {
            return Long.MAX_VALUE;
        }
    }

    /** Libellé complet (ex. A1-01). */
    public static String formatSequentialLabel(char roomLetter, int roomSequence, String bedCode) {
        String bed = bedCode == null || bedCode.isBlank() ? "01" : bedCode.strip();
        return formatRoomCode(roomLetter, roomSequence) + "-" + bed;
    }

    /** Numéro de lit dans la chambre : 01, 02, … (index 0 → 01, max 99 lits/chambre). */
    public static String bedCodeFromIndex(int index) {
        if (index < 0 || index > 98) {
            throw new IllegalArgumentException("Index de lit hors plage : " + index);
        }
        return String.format("%02d", index + 1);
    }

    /** Tri numérique du lit (01, 02 ; compat. anciens libellés lettres). */
    public static long bedOrderKey(String label) {
        String bed = parse(label).bed();
        if (bed == null || bed.isBlank()) {
            return Long.MAX_VALUE;
        }
        String digits = bed.replaceAll("\\D", "");
        if (!digits.isEmpty()) {
            try {
                return Long.parseLong(digits);
            } catch (NumberFormatException ignored) {
                return Long.MAX_VALUE;
            }
        }
        if (bed.length() == 1 && Character.isLetter(bed.charAt(0))) {
            return bed.charAt(0) - 'A' + 1;
        }
        return Long.MAX_VALUE;
    }

    public record Parsed(String room, String bed) {
    }
}
