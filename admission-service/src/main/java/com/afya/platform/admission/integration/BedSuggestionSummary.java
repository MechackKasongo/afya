package com.afya.platform.admission.integration;

public record BedSuggestionSummary(
        boolean available,
        String room,
        String bed,
        String message
) {
}
