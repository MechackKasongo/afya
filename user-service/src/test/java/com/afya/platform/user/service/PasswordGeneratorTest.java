package com.afya.platform.user.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests unitaires du générateur de mots de passe.
 * Vérifie les exigences de sécurité : longueur, contenu et déterminisme.
 */
class PasswordGeneratorTest {

    @Test
    void generatedPasswordHasRequiredLength() {
        String password = PasswordGenerator.suggest("Jean", "Dupont", "Marie", 16, 0);
        assertThat(password).hasSizeGreaterThanOrEqualTo(12);
    }

    @Test
    void generatedPasswordContainsUpperCaseLetter() {
        String password = PasswordGenerator.suggest("Jean", "Dupont", "Marie", 16, 0);
        assertThat(password).matches(".*[A-Z].*");
    }

    @Test
    void generatedPasswordContainsLowerCaseLetter() {
        String password = PasswordGenerator.suggest("Jean", "Dupont", "Marie", 16, 0);
        assertThat(password).matches(".*[a-z].*");
    }

    @Test
    void generatedPasswordContainsDigit() {
        String password = PasswordGenerator.suggest("Jean", "Dupont", "Marie", 16, 0);
        assertThat(password).matches(".*[0-9].*");
    }

    @Test
    void generatedPasswordContainsSpecialChar() {
        String password = PasswordGenerator.suggest("Jean", "Dupont", "Marie", 16, 0);
        assertThat(password).matches(".*[@#$%&*+\\-].*");
    }

    @Test
    void differentVariationProducesDifferentPassword() {
        String v0 = PasswordGenerator.suggest("Jean", "Dupont", "Marie", 16, 0);
        String v1 = PasswordGenerator.suggest("Jean", "Dupont", "Marie", 16, 1);
        assertThat(v0).isNotEqualTo(v1);
    }

    @Test
    void sameInputsProduceSamePassword() {
        String p1 = PasswordGenerator.suggest("Jean", "Dupont", "Marie", 16, 0);
        String p2 = PasswordGenerator.suggest("Jean", "Dupont", "Marie", 16, 0);
        assertThat(p1).isEqualTo(p2);
    }

    @Test
    void handlesNullPostName() {
        // Ne doit pas lever d'exception
        String password = PasswordGenerator.suggest("Alice", "Martin", null, 12, 0);
        assertThat(password).isNotBlank();
        assertThat(password).hasSizeGreaterThanOrEqualTo(12);
    }

    @Test
    void shortLengthClampedToMinimum() {
        // Longueur inférieure à 12 doit être recadrée à 12
        String password = PasswordGenerator.suggest("Alice", "Martin", "", 4, 0);
        assertThat(password).hasSizeGreaterThanOrEqualTo(12);
    }

    @Test
    void longLengthClampedToMaximum() {
        // Longueur supérieure à 24 doit être recadrée à 24
        String password = PasswordGenerator.suggest("Alice", "Martin", "", 100, 0);
        assertThat(password).hasSizeLessThanOrEqualTo(24);
    }
}
