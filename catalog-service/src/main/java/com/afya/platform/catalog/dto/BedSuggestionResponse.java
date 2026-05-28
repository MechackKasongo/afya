package com.afya.platform.catalog.dto;

public record BedSuggestionResponse(
        boolean available,
        String room,
        String bed,
        long occupiedBeds,
        int bedCapacity,
        String message
) {
}
