package com.afya.platform.identity.dto;

public record CredentialsLogPreviewResponse(
        String content,
        boolean empty,
        boolean truncated,
        long totalBytes,
        int lineCount
) {
}
