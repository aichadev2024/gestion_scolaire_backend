package com.gestionscolaire.gestion_scolaire_backend.modules.iam.services;

import com.gestionscolaire.gestion_scolaire_backend.core.exceptions.BadRequestException;
import com.gestionscolaire.gestion_scolaire_backend.core.storage.StorageService;
import com.gestionscolaire.gestion_scolaire_backend.modules.iam.models.Profil;
import com.gestionscolaire.gestion_scolaire_backend.modules.iam.repositories.ProfilRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

/**
 * Migration ponctuelle des photos de profil : celles encore stockées en
 * data-URI base64 dans {@code profils.photo_url} sont ré-uploadées vers le
 * stockage d'objets (R2), et la colonne ne garde plus que l'URL publique.
 *
 * <p>Déclenchée manuellement par un SUPER_ADMIN via
 * {@code POST /api/profils/photos/migration}. Idempotente : relancer ne
 * retouche que les profils encore en base64.
 */
@Service
public class PhotoMigrationService {

    private static final Logger log = LoggerFactory.getLogger(PhotoMigrationService.class);
    private static final String PREFIXE_DATA_URI = "data:image";

    private final ProfilRepository profilRepository;
    private final ObjectProvider<StorageService> storageProvider;

    public PhotoMigrationService(ProfilRepository profilRepository,
                                 ObjectProvider<StorageService> storageProvider) {
        this.profilRepository = profilRepository;
        this.storageProvider = storageProvider;
    }

    /**
     * @param dryRun si {@code true}, on compte seulement les profils concernés sans rien uploader ni écrire.
     * @return rapport {@code {scannes, migres, echecs, erreurs:[...]}}
     */
    public Map<String, Object> migrerPhotosBase64(boolean dryRun) {
        StorageService storage = storageProvider.getIfAvailable();
        if (storage == null && !dryRun) {
            throw new BadRequestException(
                    "Le stockage d'images n'est pas configuré. Définissez les variables R2_* avant de lancer la migration.");
        }

        List<Profil> aMigrer = profilRepository.findByPhotoUrlStartingWith(PREFIXE_DATA_URI);
        int migres = 0;
        List<String> erreurs = new ArrayList<>();

        for (Profil profil : aMigrer) {
            if (dryRun) continue;
            try {
                Decoded d = decoderDataUri(profil.getPhotoUrl());
                String url = storage.uploadImage(d.bytes(), d.contentType(), "profils/" + profil.getId());
                profil.setPhotoUrl(url);
                profilRepository.save(profil);
                migres++;
                log.info("Photo migrée pour le profil {} -> {}", profil.getId(), url);
            } catch (Exception e) {
                String msg = "profil " + profil.getId() + " : " + e.getMessage();
                erreurs.add(msg);
                log.warn("Migration photo échouée — {}", msg);
            }
        }

        return Map.of(
                "scannes", aMigrer.size(),
                "migres", migres,
                "echecs", erreurs.size(),
                "dryRun", dryRun,
                "erreurs", erreurs);
    }

    /** Découpe un data-URI {@code data:image/png;base64,iVBOR...} en type MIME + octets. */
    private Decoded decoderDataUri(String dataUri) {
        int virgule = dataUri.indexOf(',');
        if (virgule < 0) {
            throw new BadRequestException("data-URI sans séparateur ','");
        }
        String entete = dataUri.substring(5, virgule); // après "data:"
        int pointVirgule = entete.indexOf(';');
        String contentType = (pointVirgule > 0 ? entete.substring(0, pointVirgule) : entete).toLowerCase();
        byte[] bytes = Base64.getMimeDecoder().decode(dataUri.substring(virgule + 1));
        return new Decoded(contentType, bytes);
    }

    private record Decoded(String contentType, byte[] bytes) {
    }
}
