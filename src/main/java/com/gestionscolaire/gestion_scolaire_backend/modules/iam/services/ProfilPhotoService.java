package com.gestionscolaire.gestion_scolaire_backend.modules.iam.services;

import com.gestionscolaire.gestion_scolaire_backend.core.exceptions.BadRequestException;
import com.gestionscolaire.gestion_scolaire_backend.core.exceptions.ResourceNotFoundException;
import com.gestionscolaire.gestion_scolaire_backend.core.storage.StorageService;
import com.gestionscolaire.gestion_scolaire_backend.core.tenancy.TenantContext;
import com.gestionscolaire.gestion_scolaire_backend.modules.iam.models.Profil;
import com.gestionscolaire.gestion_scolaire_backend.modules.iam.models.Utilisateur;
import com.gestionscolaire.gestion_scolaire_backend.modules.iam.repositories.ProfilRepository;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/** Gestion de la photo d'un {@link Profil} : upload vers le stockage d'objets, suppression. */
@Service
public class ProfilPhotoService {

    private final ProfilRepository profilRepository;
    private final ObjectProvider<StorageService> storageProvider;

    public ProfilPhotoService(ProfilRepository profilRepository, ObjectProvider<StorageService> storageProvider) {
        this.profilRepository = profilRepository;
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
    public String uploadPhoto(Long profilId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Aucun fichier reçu.");
        }
        Profil profil = profilRepository.findById(profilId)
                .orElseThrow(() -> new ResourceNotFoundException("Profil introuvable : " + profilId));
        verifierAcces(profil);

        StorageService s = storage();
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new BadRequestException("Lecture du fichier impossible.");
        }

        String url = s.uploadImage(bytes, file.getContentType(), "profils/" + profilId);

        String ancienne = profil.getPhotoUrl();
        if (s.isManagedUrl(ancienne)) {
            s.deleteByUrl(ancienne);
        }
        profil.setPhotoUrl(url);
        profilRepository.save(profil);
        return url;
    }

    @Transactional
    public void removePhoto(Long profilId) {
        Profil profil = profilRepository.findById(profilId)
                .orElseThrow(() -> new ResourceNotFoundException("Profil introuvable : " + profilId));
        verifierAcces(profil);

        String ancienne = profil.getPhotoUrl();
        StorageService s = storageProvider.getIfAvailable();
        if (s != null && s.isManagedUrl(ancienne)) {
            s.deleteByUrl(ancienne);
        }
        profil.setPhotoUrl(null);
        profilRepository.save(profil);
    }

    /** Un utilisateur ne peut modifier que les profils de son établissement (hors SUPER_ADMIN). */
    private void verifierAcces(Profil profil) {
        if (TenantContext.isCrossTenant()) return;
        Utilisateur u = profil.getUtilisateur();
        Long tenant = TenantContext.getEtablissementId();
        if (u == null || u.getEtablissement() == null || tenant == null
                || !tenant.equals(u.getEtablissement().getId())) {
            throw new AccessDeniedException("Ce profil n'appartient pas à votre établissement.");
        }
    }
}
