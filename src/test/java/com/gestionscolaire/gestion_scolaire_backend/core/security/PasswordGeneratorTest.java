package com.gestionscolaire.gestion_scolaire_backend.core.security;

import org.junit.jupiter.api.RepeatedTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordGeneratorTest {

    @RepeatedTest(50)
    void generePasswordRobusteEtSansCaractereAmbigu() {
        String mdp = PasswordGenerator.generer();

        assertEquals(12, mdp.length());
        assertTrue(mdp.chars().anyMatch(Character::isUpperCase), "au moins une majuscule");
        assertTrue(mdp.chars().anyMatch(Character::isLowerCase), "au moins une minuscule");
        assertTrue(mdp.chars().anyMatch(Character::isDigit), "au moins un chiffre");
        assertTrue(mdp.chars().anyMatch(c -> "@#%+=?".indexOf(c) >= 0), "au moins un caractère spécial");
        assertTrue(mdp.chars().noneMatch(c -> "0O1lI".indexOf(c) >= 0), "aucun caractère ambigu");
    }

    @RepeatedTest(10)
    void deuxGenerationsDifferent() {
        assertNotEquals(PasswordGenerator.generer(), PasswordGenerator.generer());
    }
}
