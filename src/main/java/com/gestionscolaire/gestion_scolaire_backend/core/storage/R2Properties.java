package com.gestionscolaire.gestion_scolaire_backend.core.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Paramètres du bucket Cloudflare R2 (préfixe {@code storage.r2}). */
@ConfigurationProperties(prefix = "storage.r2")
public class R2Properties {

    private boolean enabled;
    private String accountId;
    private String accessKey;
    private String secretKey;
    private String bucket;
    private String publicBaseUrl;

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public String getAccountId() { return accountId; }
    public void setAccountId(String accountId) { this.accountId = accountId; }

    public String getAccessKey() { return accessKey; }
    public void setAccessKey(String accessKey) { this.accessKey = accessKey; }

    public String getSecretKey() { return secretKey; }
    public void setSecretKey(String secretKey) { this.secretKey = secretKey; }

    public String getBucket() { return bucket; }
    public void setBucket(String bucket) { this.bucket = bucket; }

    public String getPublicBaseUrl() { return publicBaseUrl; }
    public void setPublicBaseUrl(String publicBaseUrl) {
        // on tolère un slash final dans la conf
        this.publicBaseUrl = publicBaseUrl == null ? null : publicBaseUrl.replaceAll("/+$", "");
    }

    /** Endpoint S3 de l'API R2. */
    public String endpoint() {
        return "https://" + accountId + ".r2.cloudflarestorage.com";
    }
}
