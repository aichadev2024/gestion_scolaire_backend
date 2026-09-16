package com.gestionscolaire.gestion_scolaire_backend.core.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Relais public pour les images du bucket R2 (photos de profil, logos
 * d'établissement). Nécessaire car le bucket R2 ne renvoie pas d'en-têtes
 * CORS : l'export PDF des cartes scolaires (admin web) capture les cartes
 * via html2canvas, qui a besoin de lire les pixels de l'image — une simple
 * balise {@code <img>} peut l'afficher à l'écran sans CORS, mais html2canvas
 * ne peut pas la « lire » sans. On ne relaie que les URLs qui appartiennent
 * déjà à notre bucket ({@link StorageService#isManagedUrl}) pour éviter
 * d'en faire un relais ouvert vers n'importe quelle URL (SSRF).
 */
@RestController
public class ImageProxyController {

    private static final Logger log = LoggerFactory.getLogger(ImageProxyController.class);

    private final Optional<StorageService> storageService;
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    public ImageProxyController(Optional<StorageService> storageService) {
        this.storageService = storageService;
    }

    @GetMapping("/api/public/image-proxy")
    public ResponseEntity<byte[]> proxy(@RequestParam String url) {
        if (storageService.isEmpty() || !storageService.get().isManagedUrl(url)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(8))
                    .GET()
                    .build();
            HttpResponse<byte[]> upstream = http.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (upstream.statusCode() != 200) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }
            return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_JPEG)
                    .cacheControl(CacheControl.maxAge(365, TimeUnit.DAYS).cachePublic().immutable())
                    .body(upstream.body());
        } catch (Exception e) {
            log.warn("Relais image échoué pour {} : {}", url, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).build();
        }
    }
}
