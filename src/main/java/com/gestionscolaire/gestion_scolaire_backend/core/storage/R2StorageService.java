package com.gestionscolaire.gestion_scolaire_backend.core.storage;

import com.gestionscolaire.gestion_scolaire_backend.core.exceptions.BadRequestException;
import net.coobird.thumbnailator.Thumbnails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.util.Set;
import java.util.UUID;

/**
 * Implémentation Cloudflare R2 (API S3). Bean créé uniquement si
 * {@code storage.r2.enabled=true}.
 */
@Service
@EnableConfigurationProperties(R2Properties.class)
@ConditionalOnProperty(prefix = "storage.r2", name = "enabled", havingValue = "true")
public class R2StorageService implements StorageService {

    private static final Logger log = LoggerFactory.getLogger(R2StorageService.class);
    private static final Set<String> TYPES_AUTORISES = Set.of("image/jpeg", "image/jpg", "image/png", "image/webp");
    private static final int TAILLE_MAX_PX = 512;
    private static final long POIDS_MAX_OCTETS = 6L * 1024 * 1024;

    private static final java.util.Map<String, String> EXTENSIONS_DOCUMENT = java.util.Map.of(
            "application/pdf", "pdf",
            "image/jpeg", "jpg",
            "image/jpg", "jpg",
            "image/png", "png");
    private static final long POIDS_MAX_DOCUMENT_OCTETS = 6L * 1024 * 1024;

    private final R2Properties props;
    private final S3Client s3;

    public R2StorageService(R2Properties props) {
        this.props = props;
        this.s3 = S3Client.builder()
                .endpointOverride(URI.create(props.endpoint()))
                .region(Region.of("auto"))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(props.getAccessKey(), props.getSecretKey())))
                .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
                .httpClientBuilder(UrlConnectionHttpClient.builder())
                .build();
        log.info("Stockage R2 actif — bucket '{}', base publique '{}'", props.getBucket(), props.getPublicBaseUrl());
    }

    @Override
    public String uploadImage(byte[] data, String originalContentType, String keyPrefix) {
        if (data == null || data.length == 0) {
            throw new BadRequestException("Fichier vide.");
        }
        if (data.length > POIDS_MAX_OCTETS) {
            throw new BadRequestException("Image trop lourde (max 6 Mo).");
        }
        String ct = originalContentType == null ? "" : originalContentType.toLowerCase();
        if (!TYPES_AUTORISES.contains(ct)) {
            throw new BadRequestException("Format non supporté. Utilisez JPEG, PNG ou WebP.");
        }

        byte[] jpeg = redimensionnerEnJpeg(data);
        String key = keyPrefix + "/" + UUID.randomUUID() + ".jpg";

        s3.putObject(
                PutObjectRequest.builder()
                        .bucket(props.getBucket())
                        .key(key)
                        .contentType("image/jpeg")
                        .cacheControl("public, max-age=31536000, immutable")
                        .build(),
                RequestBody.fromBytes(jpeg));

        return props.getPublicBaseUrl() + "/" + key;
    }

    @Override
    public String uploadDocument(byte[] data, String originalContentType, String keyPrefix) {
        if (data == null || data.length == 0) {
            throw new BadRequestException("Fichier vide.");
        }
        if (data.length > POIDS_MAX_DOCUMENT_OCTETS) {
            throw new BadRequestException("Document trop lourd (max 6 Mo).");
        }
        String ct = originalContentType == null ? "" : originalContentType.toLowerCase();
        String extension = EXTENSIONS_DOCUMENT.get(ct);
        if (extension == null) {
            throw new BadRequestException("Format non supporté. Utilisez un PDF, JPEG ou PNG.");
        }

        String key = keyPrefix + "/" + UUID.randomUUID() + "." + extension;

        s3.putObject(
                PutObjectRequest.builder()
                        .bucket(props.getBucket())
                        .key(key)
                        .contentType(ct)
                        .cacheControl("public, max-age=31536000, immutable")
                        .build(),
                RequestBody.fromBytes(data));

        return props.getPublicBaseUrl() + "/" + key;
    }

    @Override
    public void deleteByUrl(String url) {
        if (!isManagedUrl(url)) return;
        String key = url.substring(props.getPublicBaseUrl().length() + 1);
        try {
            s3.deleteObject(DeleteObjectRequest.builder().bucket(props.getBucket()).key(key).build());
        } catch (Exception e) {
            log.warn("Suppression R2 échouée pour {} : {}", key, e.getMessage());
        }
    }

    @Override
    public boolean isManagedUrl(String url) {
        return url != null
                && props.getPublicBaseUrl() != null
                && !props.getPublicBaseUrl().isBlank()
                && url.startsWith(props.getPublicBaseUrl() + "/");
    }

    private byte[] redimensionnerEnJpeg(byte[] data) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Thumbnails.of(new ByteArrayInputStream(data))
                    .size(TAILLE_MAX_PX, TAILLE_MAX_PX)
                    .outputFormat("jpg")
                    .outputQuality(0.82)
                    .toOutputStream(out);
            byte[] result = out.toByteArray();
            if (result.length == 0) throw new IllegalStateException("sortie vide");
            return result;
        } catch (Exception e) {
            throw new BadRequestException("Image illisible ou corrompue.");
        }
    }
}
