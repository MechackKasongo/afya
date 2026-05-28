package com.afya.platform.shared.audit;

import java.util.List;
import java.util.stream.Collectors;

public final class AuditMetadata {

    private AuditMetadata() {
    }

    /** Ajoute les rôles JWT de l'acteur au moment de l'appel (avant publication asynchrone). */
    public static String enrichWithActorRoles(String metadataJson) {
        List<String> roles = AuditActorResolver.currentRoles();
        if (roles.isEmpty()) {
            return metadataJson;
        }
        String rolesJson = roles.stream()
                .map(AuditMetadata::escapeJson)
                .map(r -> "\"" + r + "\"")
                .collect(Collectors.joining(","));
        String rolesFragment = "\"actorRoles\":[" + rolesJson + "]";
        if (metadataJson == null || metadataJson.isBlank()) {
            return "{" + rolesFragment + "}";
        }
        String trimmed = metadataJson.strip();
        if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
            return trimmed.substring(0, trimmed.length() - 1) + "," + rolesFragment + "}";
        }
        return trimmed;
    }

    public static String resourceId(Long id) {
        return id == null ? null : String.valueOf(id);
    }

    public static String patientId(Long patientId) {
        return "{\"patientId\":" + patientId + "}";
    }

    public static String patientIdAnd(Long patientId, String key, Object value) {
        return "{\"patientId\":" + patientId + ",\"" + key + "\":" + value + "}";
    }

    public static String dossierNumber(String dossierNumber) {
        return "{\"dossierNumber\":\"" + escapeJson(dossierNumber) + "\"}";
    }

    public static String json(String key, Object value) {
        return "{\"" + key + "\":" + value + "}";
    }

    public static String json(String key1, Object value1, String key2, Object value2) {
        return "{\"" + key1 + "\":" + value1 + ",\"" + key2 + "\":" + value2 + "}";
    }

    private static String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
