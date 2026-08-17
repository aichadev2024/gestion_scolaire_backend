package com.gestionscolaire.gestion_scolaire_backend.modules.etablissement.services;

import com.gestionscolaire.gestion_scolaire_backend.modules.etablissement.dto.CreateEtablissementWithAdminRequest;
import com.gestionscolaire.gestion_scolaire_backend.modules.etablissement.dto.EtablissementResponse;
import com.gestionscolaire.gestion_scolaire_backend.modules.etablissement.models.StatutEtablissement;

import java.util.List;

public interface EtablissementService {
    EtablissementResponse creerEtablissementAvecAdmin(CreateEtablissementWithAdminRequest request);
    List<EtablissementResponse> listerTous();
    EtablissementResponse obtenirParId(Long id);
    EtablissementResponse modifierStatut(Long id, StatutEtablissement statut);
}


