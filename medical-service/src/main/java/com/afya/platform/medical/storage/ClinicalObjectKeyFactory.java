package com.afya.platform.medical.storage;

import java.util.UUID;

public final class ClinicalObjectKeyFactory {

    private ClinicalObjectKeyFactory() {
    }

    public static String forPatientDocument(Long patientId, String originalFilename) {
        String safeName = sanitizeFilename(originalFilename);
        return "patients/" + patientId + "/" + UUID.randomUUID() + "-" + safeName;
    }

    private static String sanitizeFilename(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank()) {
            return "document";
        }
        String name = originalFilename.strip().replace('\\', '/');
        int slash = name.lastIndexOf('/');
        if (slash >= 0) {
            name = name.substring(slash + 1);
        }
        name = name.replaceAll("[^a-zA-Z0-9._-]", "_");
        if (name.isBlank()) {
            return "document";
        }
        return name.length() > 120 ? name.substring(name.length() - 120) : name;
    }
}
