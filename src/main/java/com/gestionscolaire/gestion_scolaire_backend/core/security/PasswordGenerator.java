package com.gestionscolaire.gestion_scolaire_backend.core.security;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Génère des mots de passe initiaux robustes et lisibles pour les comptes créés
 * par l'administration (élèves, enseignants, parents…).
 *
 * <p>Caractères ambigus exclus (0/O, 1/l/I) pour faciliter la transmission orale
 * ou papier. Le mot de passe est affiché une seule fois à l'administrateur ;
 * l'utilisateur doit le changer à la première connexion.</p>
 */
public final class PasswordGenerator {

    private static final SecureRandom RNG = new SecureRandom();

    private static final String MAJUSCULES = "ABCDEFGHJKMNPQRSTUVWXYZ";
    private static final String MINUSCULES = "abcdefghijkmnpqrstuvwxyz";
    private static final String CHIFFRES = "23456789";
    private static final String SPECIAUX = "@#%+=?";
    private static final String TOUS = MAJUSCULES + MINUSCULES + CHIFFRES + SPECIAUX;

    private static final int LONGUEUR = 12;

    private PasswordGenerator() {}

    public static String generer() {
        List<Character> caracteres = new ArrayList<>(LONGUEUR);
        // Au moins un caractère de chaque classe.
        caracteres.add(tirer(MAJUSCULES));
        caracteres.add(tirer(MINUSCULES));
        caracteres.add(tirer(CHIFFRES));
        caracteres.add(tirer(SPECIAUX));
        while (caracteres.size() < LONGUEUR) {
            caracteres.add(tirer(TOUS));
        }
        Collections.shuffle(caracteres, RNG);

        StringBuilder sb = new StringBuilder(LONGUEUR);
        for (char c : caracteres) {
            sb.append(c);
        }
        return sb.toString();
    }

    private static char tirer(String source) {
        return source.charAt(RNG.nextInt(source.length()));
    }
}
