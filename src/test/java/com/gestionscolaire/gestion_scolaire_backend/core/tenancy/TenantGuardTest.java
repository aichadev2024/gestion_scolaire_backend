package com.gestionscolaire.gestion_scolaire_backend.core.tenancy;

import com.gestionscolaire.gestion_scolaire_backend.core.exceptions.ResourceNotFoundException;
import com.gestionscolaire.gestion_scolaire_backend.modules.etablissement.models.Etablissement;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TenantGuardTest {

    private final TenantGuard guard = new TenantGuard();

    /** Petite entité de test rattachée à un établissement. */
    static class FakeEntity implements TenantScoped {
        private Etablissement etablissement;
        FakeEntity(Long etablissementId) {
            if (etablissementId != null) {
                this.etablissement = Etablissement.builder().id(etablissementId).build();
            }
        }
        public Etablissement getEtablissement() { return etablissement; }
        public void setEtablissement(Etablissement e) { this.etablissement = e; }
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void memeEtablissement_estAutorise() {
        TenantContext.set(7L, false);
        FakeEntity e = new FakeEntity(7L);
        assertSame(e, guard.requireSameTenant(e));
        assertTrue(guard.appartientAuTenantCourant(e));
    }

    @Test
    void autreEtablissement_estRefuseCommeIntrouvable() {
        TenantContext.set(7L, false);
        assertThrows(ResourceNotFoundException.class, () -> guard.requireSameTenant(new FakeEntity(99L)));
        assertFalse(guard.appartientAuTenantCourant(new FakeEntity(99L)));
    }

    @Test
    void entiteSansEtablissement_estRefusee() {
        TenantContext.set(7L, false);
        assertThrows(ResourceNotFoundException.class, () -> guard.requireSameTenant(new FakeEntity(null)));
    }

    @Test
    void superAdmin_voitTousLesEtablissements() {
        TenantContext.set(null, true);
        assertTrue(guard.crossTenant());
        assertSame(FakeEntity.class, guard.requireSameTenant(new FakeEntity(1234L)).getClass());
        List<FakeEntity> liste = List.of(new FakeEntity(1L), new FakeEntity(2L), new FakeEntity(3L));
        assertEquals(3, guard.filterSameTenant(liste).size());
    }

    @Test
    void filterSameTenant_neGardeQueLeBonEtablissement() {
        TenantContext.set(2L, false);
        List<FakeEntity> liste = List.of(new FakeEntity(1L), new FakeEntity(2L), new FakeEntity(2L), new FakeEntity(3L));
        assertEquals(2, guard.filterSameTenant(liste).size());
    }

    @Test
    void contexteNonResolu_requireEtablissementId_echoue() {
        assertThrows(IllegalStateException.class, guard::requireEtablissementId);
    }
}
