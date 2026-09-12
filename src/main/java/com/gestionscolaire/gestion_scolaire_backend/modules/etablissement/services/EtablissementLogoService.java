package com.gestionscolaire.gestion_scolaire_backend.modules.etablissement.services;

import com.gestionscolaire.gestion_scolaire_backend.core.exceptions.BadRequestException;
import com.gestionscolaire.gestion_scolaire_backend.core.exceptions.ResourceNotFoundException;
import com.gestionscolaire.gestion_scolaire_backend.core.storage.StorageService;
import com.gestionscolaire.gestion_scolaire_backend.modules.etablissement.models.Etablissement;
import com.gestionscolaire.gestion_scolaire_backend.modules.etablissement.repositories.EtablissementRepository;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/** Gestion du logo d'un {@link Etablissement} : upload vers le stockage d'objets, suppression. */
@Service
public class EtablissementLogoService {

    private final EtablissementRepository etablissementRepository;
    private final ObjectProvider<StorageService> storageProvider;

    public EtablissementLogoService(EtablissementRepository etablissementRepository, ObjectProvider<StorageService> storageProvider) {
        this.etablissementRepository = etablissementRepository;
        this.storageProvider = storageProvider;
    }

    private StorageService storage() {
        StorageService s = storageProvider.getIfAvailable();
        if (s == null) {
            throw new BadRequestException(
                    "Le stockage d'images n'est pas configuré. Définissez les variables R2_* (voir docs/deploiement-production.md).");
        }
        return s;
    }

    @Transactional
    public String uploadLogo(Long etablissementId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Aucun fichier reçu.");
        }
        Etablissement etablissement = etablissementRepository.findById(etablissementId)
                .orElseThrow(() -> new ResourceNotFoundException("Établissement introuvable ID : " + etablissementId));

        StorageService s = storage();
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new BadRequestException("Lecture du fichier impossible.");
        }

        String url = s.uploadImage(bytes, file.getContentType(), "etablissements/" + etablissementId);

        String ancienne = etablissement.getLogoUrl();
        if (s.isManagedUrl(ancienne)) {
            s.deleteByUrl(ancienne);
        }
        etablissement.setLogoUrl(url);
        etablissementRepository.save(etablissement);
        return url;
    }

    @Transactional
    public void removeLogo(Long etablissementId) {
        Etablissement etablissement = etablissementRepository.findById(etablissementId)
                .orElseThrow(() -> new ResourceNotFoundException("Établissement introuvable ID : " + etablissementId));

        String ancienne = etablissement.getLogoUrl();
        StorageService s = storageProvider.getIfAvailable();
        if (s != null && s.isManagedUrl(ancienne)) {
            s.deleteByUrl(ancienne);
        }
        etablissement.setLogoUrl(null);
        etablissementRepository.save(etablissement);
    }
}
