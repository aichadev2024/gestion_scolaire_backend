package com.gestionscolaire.gestion_scolaire_backend.core.verification;

import java.security.SecureRandom;

/** Codes de vérification opaques (non devinables) apposés sur les documents officiels. */
public final class CodeGenerator {

    private static final String ALPHABET = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ"; // sans 0/O/1/I
    private static final SecureRandom RANDOM = new SecureRandom();

    private CodeGenerator() {
    }

    /** Code court et lisible (ex. impression), assez d'entropie pour ne pas être deviné. */
    public static String court() {
        StringBuilder sb = new StringBuilder(10);
        for (int i = 0; i < 10; i++) {
            sb.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }
}
