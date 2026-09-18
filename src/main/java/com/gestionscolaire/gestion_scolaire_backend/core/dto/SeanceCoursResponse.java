package com.gestionscolaire.gestion_scolaire_backend.core.dto;

import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.models.ClasseMatiere;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.models.SeanceCours;

import java.time.LocalDate;
import java.time.LocalTime;

public record SeanceCoursResponse(
        Long id,
        Long classeMatiereId,
        Long classeId,
        String classeNom,
        String matiereNom,
        String enseignantNom,
        LocalDate date,
        LocalTime heureDebut,
        LocalTime heureFin,
        boolean effectue,
        String titre,
        String contenu,
        String devoirs,
        String motifNonEffectue
) {
    public static String nomEnseignant(ClasseMatiere cm) {
        if (cm == null || cm.getEnseignant() == null || cm.getEnseignant().getProfil() == null) return null;
        return (cm.getEnseignant().getProfil().getPrenom() + " " + cm.getEnseignant().getProfil().getNom()).trim();
    }

    public static SeanceCoursResponse de(SeanceCours s) {
        ClasseMatiere cm = s.getClasseMatiere();
        return new SeanceCoursResponse(
                s.getId(),
                cm.getId(),
                cm.getClasse() != null ? cm.getClasse().getId() : null,
                cm.getClasse() != null ? cm.getClasse().getNom() : null,
                cm.getMatiere() != null ? cm.getMatiere().getNom() : null,
                nomEnseignant(cm),
                s.getDate(),
                s.getHeureDebut(),
                s.getHeureFin(),
                Boolean.TRUE.equals(s.getEffectue()),
                s.getTitre(),
                s.getContenu(),
                s.getDevoirs(),
                s.getMotifNonEffectue()
        );
    }
}
