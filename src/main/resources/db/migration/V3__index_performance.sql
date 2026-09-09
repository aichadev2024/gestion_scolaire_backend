-- =====================================================================
-- V3 — Index de performance
-- Cloisonnement multi-établissements : chaque lecture filtre sur
-- etablissement_id. Ces index évitent les balayages de table complets.
-- =====================================================================

create index if not exists idx_utilisateurs_etablissement on utilisateurs (etablissement_id);
create index if not exists idx_parents_etablissement on parents (etablissement_id);
create index if not exists idx_enseignants_etablissement on enseignants (etablissement_id);
create index if not exists idx_classes_etablissement on classes (etablissement_id);
create index if not exists idx_eleves_etablissement on eleves (etablissement_id);
create index if not exists idx_matieres_etablissement on matieres (etablissement_id);
create index if not exists idx_classes_matieres_etablissement on classes_matieres (etablissement_id);
create index if not exists idx_notes_etablissement on notes (etablissement_id);
create index if not exists idx_bulletins_etablissement on bulletins (etablissement_id);
create index if not exists idx_presences_etablissement on presences (etablissement_id);
create index if not exists idx_presences_enseignants_etablissement on presences_enseignants (etablissement_id);
create index if not exists idx_frais_scolarite_etablissement on frais_scolarite (etablissement_id);
create index if not exists idx_paiements_etablissement on paiements (etablissement_id);
create index if not exists idx_emplois_du_temps_etablissement on emplois_du_temps (etablissement_id);
create index if not exists idx_notifications_etablissement on notifications (etablissement_id);
create index if not exists idx_journaux_activites_etablissement on journaux_activites (etablissement_id);

-- Accès fréquents par relation
create index if not exists idx_eleves_classe on eleves (classe_id);
create index if not exists idx_eleves_parent on eleves (parent_id);
create index if not exists idx_notes_eleve on notes (eleve_id);
create index if not exists idx_notes_classe_matiere on notes (classe_matiere_id);
create index if not exists idx_presences_eleve on presences (eleve_id);
create index if not exists idx_paiements_eleve on paiements (eleve_id);
create index if not exists idx_frais_scolarite_classe on frais_scolarite (classe_id);
create index if not exists idx_classes_matieres_classe on classes_matieres (classe_id);
create index if not exists idx_bulletins_eleve on bulletins (eleve_id);
create index if not exists idx_notifications_destinataire on notifications (destinataire_id);
