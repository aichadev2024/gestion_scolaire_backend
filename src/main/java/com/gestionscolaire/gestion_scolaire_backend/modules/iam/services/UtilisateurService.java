package com.gestionscolaire.gestion_scolaire_backend.modules.iam.services;

import com.gestionscolaire.gestion_scolaire_backend.modules.iam.models.Utilisateur;
import com.gestionscolaire.gestion_scolaire_backend.modules.iam.models.Profil;
import java.util.List;
import java.util.Optional;

public interface UtilisateurService {
    Utilisateur inscrire(Utilisateur utilisateur, Profil profil, String nomRole);
    Utilisateur inscrirePremierAdmin(Utilisateur utilisateur, Profil profil);
    Utilisateur inscrireSuperAdmin(Utilisateur utilisateur, Profil profil);
    Optional<Utilisateur> trouverParEmail(String email);
    List<Utilisateur> listerTous();
    void modifierStatut(Long id, boolean estActif);
    Utilisateur modifierUtilisateur(Long id, Utilisateur details, Profil profilDetails, String nomRole, Integer niveauSuperviseId);
    void supprimerUtilisateur(Long id);
    /** Nomme cet utilisateur DIRECTEUR du niveau donné ; l'ancien titulaire de CE niveau (s'il y en a un) redevient Secrétaire. */
    Utilisateur nommerDirecteur(Long id, Integer niveauId);
}


