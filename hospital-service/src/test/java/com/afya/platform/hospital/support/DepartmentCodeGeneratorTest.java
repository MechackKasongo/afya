package com.afya.platform.hospital.support;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DepartmentCodeGeneratorTest {

    @Test
    void fromNameStripsAccentsAndSpaces() {
        assertEquals("PEDIATRIE", DepartmentCodeGenerator.fromName("Pédiatrie"));
        assertEquals("MEDECINEINTERNE", DepartmentCodeGenerator.fromName("Médecine interne"));
    }

    @Test
    void withSuffixAppendsNumber() {
        assertEquals("PEDIA", DepartmentCodeGenerator.withSuffix("PEDIA", 1));
        assertEquals("PEDIA2", DepartmentCodeGenerator.withSuffix("PEDIA", 2));
    }
}
