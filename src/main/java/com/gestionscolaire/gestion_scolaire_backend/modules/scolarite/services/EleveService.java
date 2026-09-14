package com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.services;

import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.models.Eleve;
import com.gestionscolaire.gestion_scolaire_backend.modules.iam.models.Profil;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.dto.EleveImportRapport;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.dto.PromotionRapport;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
import java.util.Optional;

public interface EleveService {
    Eleve inscrireEleve(Eleve eleve, Profil profil, Long parentId, Long classeId, String motDePasseInitial);
    Eleve modifierEleve(Long id, Eleve eleveDetails, Profil profilDetails);
    Optional<Eleve> trouverParId(Long id);
    Optional<Eleve> trouverParMatricule(String matricule);
    List<Eleve> listerElevesParClasse(Long classeId);
    List<Eleve> listerElevesParParent(Long parentId);
    List<Eleve> listerTous();
    void archiverEleve(Long id);
    void supprimerEleve(Long id);
    Eleve modifierStatutInscription(Long id, String statutInscription);
    EleveImportRapport importerDepuisExcel(MultipartFile fichier, Long classeIdParDefaut);
    byte[] genererModeleImportExcel();
    /** Fait passer les élèves sélectionnés dans une autre classe (ex. passage en classe supérieure). */
    PromotionRapport promouvoir(Long classeDestinationId, List<Long> eleveIds);
    /** Récapitulatif Excel de fin d'année (moyenne, présence, statut) — classeId null = tout l'établissement. */
    byte[] genererRecapitulatifAnnuel(Long classeId, String anneeScolaire);
}


