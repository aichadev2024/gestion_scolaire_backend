package com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.services;

import com.gestionscolaire.gestion_scolaire_backend.core.exceptions.BadRequestException;
import com.gestionscolaire.gestion_scolaire_backend.core.exceptions.ResourceNotFoundException;
import com.gestionscolaire.gestion_scolaire_backend.core.tenancy.TenantContext;
import com.gestionscolaire.gestion_scolaire_backend.modules.etablissement.models.Etablissement;
import com.gestionscolaire.gestion_scolaire_backend.modules.etablissement.repositories.EtablissementRepository;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.models.Niveau;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.repositories.NiveauRepository;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.services.NiveauService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class NiveauServiceImpl implements NiveauService {

    private final NiveauRepository niveauRepository;
    private final EtablissementRepository etablissementRepository;

    public NiveauServiceImpl(NiveauRepository niveauRepository, EtablissementRepository etablissementRepository) {
        this.niveauRepository = niveauRepository;
        this.etablissementRepository = etablissementRepository;
    }

    @Override
    public Niveau creer(String nom) {
        if (niveauRepository.findByNom(nom).isPresent()) {
            throw new BadRequestException("Un niveau avec ce nom existe déjà");
        }
        Niveau niveau = niveauRepository.save(Niveau.builder().nom(nom).build());
        // Si ce compte est rattaché à un établissement dont les niveaux sont restreints,
        // le nouveau niveau lui devient automatiquement disponible (école qui s'agrandit).
        if (!TenantContext.isCrossTenant()) {
            Long etablissementId = TenantContext.getEtablissementId();
            if (etablissementId != null) {
                etablissementRepository.findById(etablissementId).ifPresent(etablissement -> {
                    if (!etablissement.getNiveauxAutorises().isEmpty()) {
                        etablissement.getNiveauxAutorises().add(niveau);
                        etablissementRepository.save(etablissement);
                    }
                });
            }
        }
        return niveau;
    }

    @Override
    public Optional<Niveau> trouverParId(Integer id) {
        return niveauRepository.findById(id);
    }

    @Override
    public List<Niveau> listerTous() {
        // SUPER_ADMIN (cross-tenant) voit tout le catalogue global — nécessaire pour choisir
        // les niveaux proposés à un établissement (création/édition).
        if (TenantContext.isCrossTenant()) {
            return niveauRepository.findAll();
        }
        Long etablissementId = TenantContext.getEtablissementId();
        if (etablissementId == null) {
            return niveauRepository.findAll();
        }
        Etablissement etablissement = etablissementRepository.findById(etablissementId).orElse(null);
        if (etablissement == null || etablissement.getNiveauxAutorises().isEmpty()) {
            // Pas de restriction posée pour cet établissement → comportement historique (tout voir).
            return niveauRepository.findAll();
        }
        return etablissement.getNiveauxAutorises();
    }
}


