package com.afya.platform.bff.dto;

public record CredentialsLogPreviewResponse(
        String content,
        boolean empty,
        boolean truncated,
        long totalBytes,
        int lineCount
) {
}
