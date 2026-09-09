package com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.services;

import com.gestionscolaire.gestion_scolaire_backend.core.exceptions.BadRequestException;
import com.gestionscolaire.gestion_scolaire_backend.core.exceptions.ResourceNotFoundException;
import com.gestionscolaire.gestion_scolaire_backend.modules.etablissement.models.*;
import com.gestionscolaire.gestion_scolaire_backend.modules.iam.models.*;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.models.*;
import com.gestionscolaire.gestion_scolaire_backend.modules.evaluation.models.*;
import com.gestionscolaire.gestion_scolaire_backend.modules.comptabilite.models.*;
import com.gestionscolaire.gestion_scolaire_backend.modules.etablissement.repositories.*;
import com.gestionscolaire.gestion_scolaire_backend.modules.iam.repositories.*;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.repositories.*;
import com.gestionscolaire.gestion_scolaire_backend.modules.evaluation.repositories.*;
import com.gestionscolaire.gestion_scolaire_backend.modules.comptabilite.repositories.*;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.services.PresenceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@Transactional
public class PresenceServiceImpl implements PresenceService {

    @Autowired
    private PresenceRepository presenceRepository;

    @Autowired
    private EleveRepository eleveRepository;

    @Autowired
    private ClasseMatiereRepository classeMatiereRepository;

    @Autowired
    private com.gestionscolaire.gestion_scolaire_backend.core.tenancy.TenantGuard tenantGuard;

    @Override
    public Presence enregistrerPresence(Presence presence, Long eleveId, Long classeMatiereId) {
        Eleve eleve = tenantGuard.requireSameTenant(eleveRepository.findById(eleveId)
                .orElseThrow(() -> new ResourceNotFoundException("Élève introuvable")));

        if (classeMatiereId != null) {
            ClasseMatiere classeMatiere = tenantGuard.requireSameTenant(classeMatiereRepository.findById(classeMatiereId)
                    .orElseThrow(() -> new ResourceNotFoundException("ClasseMatiere introuvable")));
            presence.setClasseMatiere(classeMatiere);
        }

        presence.setEleve(eleve);
        presence.setEtablissement(eleve.getEtablissement());
        return presenceRepository.save(presence);
    }

    @Override
    public List<Presence> listerPresencesEleve(Long eleveId) {
        return tenantGuard.filterSameTenant(presenceRepository.findByEleveId(eleveId));
    }

    @Override
    public List<Presence> listerPresencesParClasseMatiereEtDate(Long classeMatiereId, LocalDate date) {
        return tenantGuard.filterSameTenant(presenceRepository.findByClasseMatiereIdAndDate(classeMatiereId, date));
    }
}


