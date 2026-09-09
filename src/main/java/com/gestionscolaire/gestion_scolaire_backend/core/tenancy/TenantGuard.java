package com.gestionscolaire.gestion_scolaire_backend.core.tenancy;

import com.gestionscolaire.gestion_scolaire_backend.core.exceptions.ResourceNotFoundException;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;

/**
 * Aide au cloisonnement par établissement dans la couche service.
 *
 * <ul>
 *   <li>{@link #crossTenant()} : l'appelant (SUPER_ADMIN) voit tous les établissements.</li>
 *   <li>{@link #etablissementId()} : établissement de l'appelant, ou {@code null} si cross-tenant.</li>
 *   <li>{@link #requireSameTenant(TenantScoped)} : renvoie l'entité si elle appartient à
 *       l'établissement de l'appelant, sinon lève {@link ResourceNotFoundException}
 *       (on ne divulgue pas l'existence d'une ressource d'un autre établissement).</li>
 *   <li>{@link #filterSameTenant(Collection)} : ne conserve que les entités du bon établissement.</li>
 * </ul>
 */
@Component
public class TenantGuard {

    public boolean crossTenant() {
        return TenantContext.isCrossTenant();
    }

    public Long etablissementId() {
        return TenantContext.getEtablissementId();
    }

    /** Établissement de l'appelant, obligatoire (échoue pour un cross-tenant ou un contexte non résolu). */
    public Long requireEtablissementId() {
        return TenantContext.requireEtablissementId();
    }

    public boolean appartientAuTenantCourant(TenantScoped entity) {
        if (entity == null) {
            return false;
        }
        if (crossTenant()) {
            return true;
        }
        Long mien = TenantContext.getEtablissementId();
        Long sien = (entity.getEtablissement() != null) ? entity.getEtablissement().getId() : null;
        return mien != null && mien.equals(sien);
    }

    public <T extends TenantScoped> T requireSameTenant(T entity) {
        if (entity == null) {
            throw new ResourceNotFoundException("Ressource introuvable");
        }
        if (!appartientAuTenantCourant(entity)) {
            throw new ResourceNotFoundException("Ressource introuvable");
        }
        return entity;
    }

    public <T extends TenantScoped> List<T> filterSameTenant(Collection<T> entities) {
        if (entities == null) {
            return List.of();
        }
        if (crossTenant()) {
            return List.copyOf(entities);
        }
        return entities.stream().filter(this::appartientAuTenantCourant).toList();
    }
}
