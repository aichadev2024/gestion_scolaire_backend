package com.gestionscolaire.gestion_scolaire_backend.core.dto;

import com.gestionscolaire.gestion_scolaire_backend.modules.iam.models.Profil;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.models.ClasseMatiere;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.models.Eleve;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.models.IncidentDiscipline;

import java.time.LocalDate;
import java.time.LocalTime;

/** Réponse d'une fiche de discipline — volontairement sans aucune entité (pas de compte, mot de passe ni OTP). */
public record IncidentDisciplineResponse(
        Long id,
        LocalDate date,
        LocalTime heure,
        String statut,
        String commentaire,
        boolean estTraite,
        String notesTraitement,
        Long classeId,
        EleveRef eleve,
        ClasseMatiereRef classeMatiere,
        String enregistreParNom
) {
    public record ProfilRef(String nom, String prenom) {}

    public record EleveRef(Long id, String matricule, ProfilRef profil) {}

    public record MatiereRef(String nom) {}

    public record EnseignantRef(ProfilRef profil) {}

    public record ClasseMatiereRef(Long id, MatiereRef matiere, EnseignantRef enseignant) {}

    private static ProfilRef profilRef(Profil p) {
        return p == null ? null : new ProfilRef(p.getNom(), p.getPrenom());
    }

    public static IncidentDisciplineResponse de(IncidentDiscipline i) {
        Eleve e = i.getEleve();
        EleveRef eleve = e == null ? null : new EleveRef(e.getId(), e.getMatricule(), profilRef(e.getProfil()));

        ClasseMatiere cm = i.getClasseMatiere();
        ClasseMatiereRef classeMatiere = null;
        if (cm != null) {
            MatiereRef matiere = cm.getMatiere() == null ? null : new MatiereRef(cm.getMatiere().getNom());
            EnseignantRef enseignant = cm.getEnseignant() == null ? null : new EnseignantRef(profilRef(cm.getEnseignant().getProfil()));
            classeMatiere = new ClasseMatiereRef(cm.getId(), matiere, enseignant);
        }

        return new IncidentDisciplineResponse(
                i.getId(),
                i.getDate(),
                i.getHeure(),
                i.getStatut(),
                i.getCommentaire(),
                Boolean.TRUE.equals(i.getEstTraite()),
                i.getNotesTraitement(),
                i.getClasse() != null ? i.getClasse().getId() : null,
                eleve,
                classeMatiere,
                i.getEnregistrePar() != null ? i.getEnregistrePar().getUsername() : null
        );
    }
}
