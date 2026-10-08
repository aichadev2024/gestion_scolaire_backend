package com.gestionscolaire.gestion_scolaire_backend.modules.evaluation.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/** Rapport de période (trimestre, composition…) d'une classe ou de tout l'établissement, calculé à la demande. */
public record RapportTrimestrielResponse(
        Entete entete,
        String portee,
        Long classeId,
        String periode,
        String anneeScolaire,
        LocalDate dateDebut,
        LocalDate dateFin,
        String commentaire,
        LocalDateTime genereLe,
        Effectifs effectifs,
        Presences presences,
        Resultats resultats,
        List<LigneMatiere> matieres,
        List<LigneClasse> classes,
        List<EleveClassement> meilleurs,
        List<EleveClassement> enDifficulte,
        Discipline discipline,
        Finances finances) {

    /** En-tête officiel de l'école (nom, logo, rive, CAP…). */
    public record Entete(String nom, String slogan, String adresse, String telephone, String email,
                         String logoUrl, String rive, String cap) {}

    public record Effectifs(int total, int garcons, int filles) {}

    /** Retards comptés comme présents ; tauxPresence nul = aucune présence saisie sur la période. */
    public record Presences(int presents, int retards, int absencesJustifiees, int absencesNonJustifiees,
                            Double tauxPresence) {}

    /** Statistiques sur les élèves ayant au moins une note sur la période (les autres sont « non évalués »). */
    public record Resultats(int evalues, int nonEvalues, Double moyenne, Double moyenneMin, Double moyenneMax,
                            Double tauxReussite, Map<String, Integer> mentions) {}

    public record LigneMatiere(String matiere, String enseignant, double coefficient, Double moyenne,
                               Double tauxReussite, int eleves) {}

    public record LigneClasse(Long classeId, String classe, int effectif, int evalues, Double moyenne,
                              Double tauxReussite, Double tauxPresence, Double tauxRecouvrement) {}

    public record EleveClassement(String nom, String classe, double moyenne) {}

    /**
     * Situation financière des élèves concernés, à ce jour (hors dates du rapport, sauf {@code encaisseSurPeriode}).
     * Montants dans la devise de l'école. Nul pour qui n'a pas accès aux finances.
     */
    public record Finances(String devise, double attendu, double encaisse, double reste, Double tauxRecouvrement,
                           double arrieres, double montantEnRetard, int elevesEnRetard, double encaisseSurPeriode) {}

    public record Discipline(int total, Map<String, Integer> parStatut, int nonTraites) {}
}
