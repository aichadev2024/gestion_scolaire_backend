package com.gestionscolaire.gestion_scolaire_backend.core.dto;

/** Une matière d'une classe assignée à l'enseignant connecté (pour saisir son cahier de texte). */
public record MonCoursResponse(Long classeMatiereId, Long classeId, String classeNom, String matiereNom, String niveauNom) {}
