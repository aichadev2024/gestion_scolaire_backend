package com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentEleveResponse {
    private Long id;
    private Long eleveId;
    private String type;
    private String libelle;
    private String url;
    private String contentType;
    private Long tailleOctets;
    private LocalDateTime dateAjout;
}
