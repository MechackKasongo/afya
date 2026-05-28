package com.afya.platform.careentry.integration;

public record BedSuggestionSummary(
        boolean available,
        String room,
        String bed,
        String message
) {
}
