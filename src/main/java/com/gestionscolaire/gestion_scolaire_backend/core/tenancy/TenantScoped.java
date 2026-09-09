package com.gestionscolaire.gestion_scolaire_backend.core.tenancy;

import com.gestionscolaire.gestion_scolaire_backend.modules.etablissement.models.Etablissement;

/**
 * Implémentée par toute entité rattachée à un établissement (tenant).
 *
 * <p>Les accesseurs sont fournis par Lombok {@code @Getter/@Setter} sur le champ
 * {@code private Etablissement etablissement;}. {@link TenantEntityListener} s'appuie
 * dessus pour renseigner automatiquement l'établissement à la persistance.</p>
 */
public interface TenantScoped {
    Etablissement getEtablissement();
    void setEtablissement(Etablissement etablissement);
}
