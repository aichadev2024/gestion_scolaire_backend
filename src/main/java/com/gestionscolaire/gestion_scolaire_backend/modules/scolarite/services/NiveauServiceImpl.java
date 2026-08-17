package com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.services;

import com.gestionscolaire.gestion_scolaire_backend.core.exceptions.BadRequestException;
import com.gestionscolaire.gestion_scolaire_backend.core.exceptions.ResourceNotFoundException;
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

    public NiveauServiceImpl(NiveauRepository niveauRepository) {
        this.niveauRepository = niveauRepository;
    }

    @Override
    public Niveau creer(String nom) {
        if (niveauRepository.findByNom(nom).isPresent()) {
            throw new BadRequestException("Un niveau avec ce nom existe déjà");
        }
        return niveauRepository.save(Niveau.builder().nom(nom).build());
    }

    @Override
    public Optional<Niveau> trouverParId(Integer id) {
        return niveauRepository.findById(id);
    }

    @Override
    public List<Niveau> listerTous() {
        return niveauRepository.findAll();
    }
}


