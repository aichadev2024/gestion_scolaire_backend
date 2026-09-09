package com.gestionscolaire.gestion_scolaire_backend.core.tenancy;

import com.gestionscolaire.gestion_scolaire_backend.modules.etablissement.models.Etablissement;
import jakarta.persistence.PrePersist;

/**
 * Renseigne automatiquement l'établissement d'une entité {@link TenantScoped} au moment
 * de la persistance, à partir de {@link TenantContext}, si l'appelant ne l'a pas déjà fixé.
 *
 * <p>Filet de sécurité : les entités créées dans le contexte d'un établissement donné
 * ne peuvent pas « fuiter » vers un autre tenant faute d'affectation explicite.</p>
 */
public class TenantEntityListener {

    @PrePersist
    public void avantPersistance(Object entity) {
        if (!(entity instanceof TenantScoped scoped)) {
            return;
        }
        if (scoped.getEtablissement() != null) {
            return;
        }
        Long etablissementId = TenantContext.getEtablissementId();
        if (etablissementId != null) {
            // Référence détachée : Hibernate écrit simplement la clé étrangère.
            scoped.setEtablissement(Etablissement.builder().id(etablissementId).build());
        }
    }
}
