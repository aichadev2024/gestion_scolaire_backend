package com.gestionscolaire.gestion_scolaire_backend.core.tenancy;

/**
 * Contexte d'établissement (tenant) courant, porté par le thread de la requête.
 *
 * <p>Alimenté par {@link TenantContextFilter} à partir du JWT :
 * <ul>
 *   <li>rôle {@code SUPER_ADMIN} → {@code crossTenant = true} (accès inter-établissements) ;</li>
 *   <li>tout autre rôle → {@code etablissementId} = établissement de l'utilisateur.</li>
 * </ul>
 *
 * <p>Les services de lecture doivent : renvoyer toutes les données si {@link #isCrossTenant()},
 * sinon filtrer sur {@link #getEtablissementId()}. En écriture, {@link TenantEntityListener}
 * renseigne automatiquement l'établissement des entités {@link TenantScoped}.</p>
 */
public final class TenantContext {

    private record Holder(Long etablissementId, boolean crossTenant) {}

    private static final ThreadLocal<Holder> HOLDER = new ThreadLocal<>();

    private TenantContext() {}

    public static void set(Long etablissementId, boolean crossTenant) {
        HOLDER.set(new Holder(etablissementId, crossTenant));
    }

    public static void clear() {
        HOLDER.remove();
    }

    /** Vrai si le contexte a été résolu pour la requête courante (utilisateur authentifié). */
    public static boolean isResolved() {
        return HOLDER.get() != null;
    }

    /** Vrai pour un SUPER_ADMIN : aucune restriction d'établissement. */
    public static boolean isCrossTenant() {
        Holder h = HOLDER.get();
        return h != null && h.crossTenant();
    }

    /** Établissement courant, ou {@code null} si non résolu / cross-tenant / utilisateur sans établissement. */
    public static Long getEtablissementId() {
        Holder h = HOLDER.get();
        return h == null ? null : h.etablissementId();
    }

    /**
     * Établissement courant, obligatoire. Lève une exception si le contexte n'est pas résolu
     * ou si l'utilisateur n'a pas d'établissement (et n'est pas cross-tenant).
     */
    public static Long requireEtablissementId() {
        Holder h = HOLDER.get();
        if (h == null || h.crossTenant()) {
            throw new IllegalStateException("Contexte d'établissement requis mais absent pour cette requête.");
        }
        if (h.etablissementId() == null) {
            throw new IllegalStateException("L'utilisateur courant n'est rattaché à aucun établissement.");
        }
        return h.etablissementId();
    }
}
