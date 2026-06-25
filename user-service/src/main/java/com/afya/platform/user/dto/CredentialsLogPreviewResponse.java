package com.afya.platform.user.dto;

public record CredentialsLogPreviewResponse(
        String content,
        boolean empty,
        boolean truncated,
        long totalBytes,
        int lineCount
) {
}
