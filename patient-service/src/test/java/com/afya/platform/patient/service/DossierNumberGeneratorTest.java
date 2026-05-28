package com.afya.platform.patient.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DossierNumberGeneratorTest {

    @Test
    void formatDossierNumber_usesCompactPattern() {
        assertEquals("D2026AAAA001", DossierNumberGenerator.formatDossierNumber(2026, "AAAA", 1));
        assertEquals("D2026BBBB042", DossierNumberGenerator.formatDossierNumber(2026, "BBBB", 42));
        assertEquals("D2027ZZZZ999", DossierNumberGenerator.formatDossierNumber(2027, "ZZZZ", 999));
    }
}
