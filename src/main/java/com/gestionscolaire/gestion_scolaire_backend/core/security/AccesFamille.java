package com.gestionscolaire.gestion_scolaire_backend.core.security;

import com.gestionscolaire.gestion_scolaire_backend.core.exceptions.ResourceNotFoundException;
import com.gestionscolaire.gestion_scolaire_backend.modules.iam.models.Utilisateur;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.models.Eleve;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.models.Parent;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;

/**
 * Cloisonnement « famille » : un PARENT ne voit que ses enfants et un ÉLÈVE que lui-même.
 * Le personnel (direction, secrétariat, enseignants…) n'est pas concerné — son périmètre
 * reste géré par {@code TenantGuard} (établissement + niveau).
 *
 * Sans cette vérification, n'importe quel parent d'un établissement pouvait lire les notes,
 * bulletins, paiements ou la fiche de n'importe quel élève du même établissement en changeant
 * simplement l'identifiant dans l'URL.
 */
@Component
public class AccesFamille {

    private Utilisateur courant() {
        try {
            return SecurityUtils.getCurrentUser().getUtilisateur();
        } catch (Exception e) {
            return null; // appel interne sans contexte d'authentification
        }
    }

    private static String role(Utilisateur u) {
        return u != null && u.getRole() != null ? u.getRole().getNom() : "";
    }

    /** Vrai si l'appelant est un parent ou un élève (donc limité à sa propre famille). */
    public boolean restreint() {
        String r = role(courant());
        return "PARENT".equalsIgnoreCase(r) || "ELEVE".equalsIgnoreCase(r);
    }

    /** Vrai si l'élève peut être vu par l'appelant : toujours vrai pour le personnel. */
    public boolean concerne(Eleve eleve) {
        Utilisateur u = courant();
        String r = role(u);
        if ("PARENT".equalsIgnoreCase(r)) {
            return estCompteDe(eleve.getParent(), u) || estCompteDe(eleve.getParentSecondaire(), u);
        }
        if ("ELEVE".equalsIgnoreCase(r)) {
            return eleve.getProfil() != null && eleve.getProfil().getUtilisateur() != null
                    && u.getId().equals(eleve.getProfil().getUtilisateur().getId());
        }
        return true;
    }

    /** Lève « introuvable » (on ne confirme pas l'existence) si l'élève n'est pas dans la famille de l'appelant. */
    public Eleve verifier(Eleve eleve) {
        if (eleve == null || !concerne(eleve)) {
            throw new ResourceNotFoundException("Élève introuvable");
        }
        return eleve;
    }

    public List<Eleve> filtrer(Collection<Eleve> eleves) {
        if (eleves == null) return List.of();
        if (!restreint()) return List.copyOf(eleves);
        return eleves.stream().filter(this::concerne).toList();
    }

    private static boolean estCompteDe(Parent parent, Utilisateur courant) {
        return parent != null && parent.getProfil() != null && parent.getProfil().getUtilisateur() != null
                && courant.getId().equals(parent.getProfil().getUtilisateur().getId());
    }
}
