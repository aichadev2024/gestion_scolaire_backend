package com.gestionscolaire.gestion_scolaire_backend.core.storage;

/** Stockage d'objets pour les médias (photos de profil). */
public interface StorageService {

    /**
     * Redimensionne l'image puis l'envoie dans le bucket.
     *
     * @param data           octets bruts du fichier reçu
     * @param originalContentType type MIME déclaré (image/jpeg, image/png, image/webp…)
     * @param keyPrefix      préfixe de clé, ex. {@code "profils/42"}
     * @return URL publique de l'image
     */
    String uploadImage(byte[] data, String originalContentType, String keyPrefix);

    /** Supprime l'objet si l'URL pointe vers notre bucket (sans effet sinon). */
    void deleteByUrl(String url);

    /** Vrai si l'URL est servie par notre bucket public. */
    boolean isManagedUrl(String url);
}
