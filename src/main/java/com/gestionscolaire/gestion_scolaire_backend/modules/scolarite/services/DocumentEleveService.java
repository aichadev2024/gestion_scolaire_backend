package com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.services;

import com.gestionscolaire.gestion_scolaire_backend.core.exceptions.BadRequestException;
import com.gestionscolaire.gestion_scolaire_backend.core.exceptions.ResourceNotFoundException;
import com.gestionscolaire.gestion_scolaire_backend.core.storage.StorageService;
import com.gestionscolaire.gestion_scolaire_backend.core.tenancy.TenantGuard;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.dto.DocumentEleveResponse;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.models.DocumentEleve;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.models.Eleve;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.repositories.DocumentEleveRepository;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.repositories.EleveRepository;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Set;

/** Documents du dossier d'inscription d'un élève (acte de naissance, etc.). */
@Service
public class DocumentEleveService {

    private static final Set<String> TYPES_CONNUS = Set.of("ACTE_NAISSANCE", "CERTIFICAT_MEDICAL", "AUTRE");

    private final DocumentEleveRepository documentEleveRepository;
    private final EleveRepository eleveRepository;
    private final ObjectProvider<StorageService> storageProvider;
    private final TenantGuard tenantGuard;

    public DocumentEleveService(DocumentEleveRepository documentEleveRepository,
                                 EleveRepository eleveRepository,
                                 ObjectProvider<StorageService> storageProvider,
                                 TenantGuard tenantGuard) {
        this.documentEleveRepository = documentEleveRepository;
        this.eleveRepository = eleveRepository;
        this.storageProvider = storageProvider;
        this.tenantGuard = tenantGuard;
    }

    private StorageService storage() {
        StorageService s = storageProvider.getIfAvailable();
        if (s == null) {
            throw new BadRequestException(
                    "Le stockage de documents n'est pas configuré. Définissez les variables R2_* (voir docs/deploiement-production.md).");
        }
        return s;
    }

    private Eleve eleveDuTenant(Long eleveId) {
        return tenantGuard.requireSameTenant(eleveRepository.findById(eleveId)
                .orElseThrow(() -> new ResourceNotFoundException("Élève introuvable ID : " + eleveId)));
    }

    @Transactional
    public DocumentEleveResponse ajouter(Long eleveId, String type, String libelle, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Aucun fichier reçu.");
        }
        Eleve eleve = eleveDuTenant(eleveId);

        String typeRetenu = (type == null || !TYPES_CONNUS.contains(type.toUpperCase())) ? "AUTRE" : type.toUpperCase();
        String libelleRetenu = (libelle == null || libelle.isBlank()) ? libelleParDefaut(typeRetenu) : libelle.trim();

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new BadRequestException("Lecture du fichier impossible.");
        }

        String url = storage().uploadDocument(bytes, file.getContentType(), "documents/" + eleveId);

        DocumentEleve doc = DocumentEleve.builder()
                .eleve(eleve)
                .etablissement(eleve.getEtablissement())
                .type(typeRetenu)
                .libelle(libelleRetenu)
                .url(url)
                .contentType(file.getContentType())
                .tailleOctets(file.getSize())
                .build();
        doc = documentEleveRepository.save(doc);

        return versDto(doc);
    }

    public List<DocumentEleveResponse> lister(Long eleveId) {
        eleveDuTenant(eleveId); // vérifie le tenant avant de révéler la liste
        return documentEleveRepository.findByEleveIdOrderByDateAjoutDesc(eleveId).stream()
                .map(this::versDto)
                .toList();
    }

    @Transactional
    public void supprimer(Long eleveId, Long documentId) {
        eleveDuTenant(eleveId);
        DocumentEleve doc = documentEleveRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document introuvable ID : " + documentId));
        if (doc.getEleve() == null || !doc.getEleve().getId().equals(eleveId)) {
            throw new ResourceNotFoundException("Document introuvable ID : " + documentId);
        }
        StorageService s = storageProvider.getIfAvailable();
        if (s != null && s.isManagedUrl(doc.getUrl())) {
            s.deleteByUrl(doc.getUrl());
        }
        documentEleveRepository.delete(doc);
    }

    private String libelleParDefaut(String type) {
        return switch (type) {
            case "ACTE_NAISSANCE" -> "Acte de naissance";
            case "CERTIFICAT_MEDICAL" -> "Certificat médical";
            default -> "Document";
        };
    }

    private DocumentEleveResponse versDto(DocumentEleve d) {
        return DocumentEleveResponse.builder()
                .id(d.getId())
                .eleveId(d.getEleve() != null ? d.getEleve().getId() : null)
                .type(d.getType())
                .libelle(d.getLibelle())
                .url(d.getUrl())
                .contentType(d.getContentType())
                .tailleOctets(d.getTailleOctets())
                .dateAjout(d.getDateAjout())
                .build();
    }
}
