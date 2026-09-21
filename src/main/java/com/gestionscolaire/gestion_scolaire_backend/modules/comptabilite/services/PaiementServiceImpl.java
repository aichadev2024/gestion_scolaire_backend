package com.gestionscolaire.gestion_scolaire_backend.modules.comptabilite.services;

import com.gestionscolaire.gestion_scolaire_backend.core.exceptions.ResourceNotFoundException;
import com.gestionscolaire.gestion_scolaire_backend.modules.comptabilite.dto.RetardPaiementResponse;
import com.gestionscolaire.gestion_scolaire_backend.modules.etablissement.models.*;
import com.gestionscolaire.gestion_scolaire_backend.modules.iam.models.*;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.models.*;
import com.gestionscolaire.gestion_scolaire_backend.modules.evaluation.models.*;
import com.gestionscolaire.gestion_scolaire_backend.modules.comptabilite.models.*;
import com.gestionscolaire.gestion_scolaire_backend.modules.etablissement.repositories.*;
import com.gestionscolaire.gestion_scolaire_backend.modules.iam.repositories.*;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.repositories.*;
import com.gestionscolaire.gestion_scolaire_backend.modules.evaluation.repositories.*;
import com.gestionscolaire.gestion_scolaire_backend.modules.comptabilite.repositories.*;
import com.gestionscolaire.gestion_scolaire_backend.modules.comptabilite.services.PaiementService;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.services.NotificationService;
import com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.services.RecuPdfService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class PaiementServiceImpl implements PaiementService {

    @Autowired
    private PaiementRepository paiementRepository;

    @Autowired
    private EleveRepository eleveRepository;

    @Autowired
    private FraisScolariteRepository fraisScolariteRepository;

    @Autowired
    private UtilisateurRepository utilisateurRepository;

    @Autowired
    private com.gestionscolaire.gestion_scolaire_backend.core.tenancy.TenantGuard tenantGuard;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private RecuPdfService recuPdfService;

    @Autowired
    private com.gestionscolaire.gestion_scolaire_backend.core.services.EmailService emailService;

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(PaiementServiceImpl.class);

    private Integer niveauDe(Eleve e) {
        return (e.getClasse() != null && e.getClasse().getNiveau() != null) ? e.getClasse().getNiveau().getId() : null;
    }

    @Override
    public Paiement enregistrerPaiement(Paiement paiement, Long eleveId, Long fraisId, Long userReceptionnaireId) {
        Eleve eleve = tenantGuard.requireSameTenant(eleveRepository.findById(eleveId)
                .orElseThrow(() -> new ResourceNotFoundException("Élève introuvable")));
        tenantGuard.requireSameNiveau(eleve, this::niveauDe);

        if (fraisId != null) {
            FraisScolarite frais = tenantGuard.requireSameTenant(fraisScolariteRepository.findById(fraisId)
                    .orElseThrow(() -> new ResourceNotFoundException("Frais de scolarité introuvables")));
            paiement.setFraisScolarite(frais);
        }

        if (userReceptionnaireId != null) {
            Utilisateur receptionnaire = utilisateurRepository.findById(userReceptionnaireId).orElse(null);
            paiement.setRecuPar(receptionnaire);
        }

        // Génération d'un numéro de reçu unique
        String receiptNum = "REC-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 5).toUpperCase();
        paiement.setNumeroRecu(receiptNum);
        paiement.setEleve(eleve);
        paiement.setEtablissement(eleve.getEtablissement());

        Paiement saved = paiementRepository.save(paiement);
        envoyerRecuAuxParents(saved, userReceptionnaireId);
        return saved;
    }

    /**
     * Remet le reçu aux parents dès l'enregistrement du paiement : notification (push sur l'appli
     * mobile) et e-mail avec le PDF en pièce jointe. Ne fait jamais échouer le paiement.
     */
    private void envoyerRecuAuxParents(Paiement paiement, Long expediteurId) {
        try {
            Eleve eleve = paiement.getEleve();
            List<Parent> parents = java.util.stream.Stream.of(eleve.getParent(), eleve.getParentSecondaire())
                    .filter(java.util.Objects::nonNull)
                    .toList();
            if (parents.isEmpty()) {
                return;
            }

            String devise = "FCFA";
            String etablissementNom = "Votre établissement";
            if (eleve.getEtablissement() != null) {
                etablissementNom = eleve.getEtablissement().getNom();
                String d = eleve.getEtablissement().getDevise();
                if (d != null && !d.isBlank()) devise = d;
            }
            java.text.DecimalFormatSymbols symbols = new java.text.DecimalFormatSymbols(java.util.Locale.FRANCE);
            symbols.setGroupingSeparator(' ');
            String montant = new java.text.DecimalFormat("#,##0.##", symbols).format(paiement.getMontantPaye()) + " " + devise;
            String nomEleve = eleve.getProfil() != null
                    ? (eleve.getProfil().getPrenom() + " " + eleve.getProfil().getNom()).trim()
                    : "votre enfant";
            String numeroRecu = paiement.getNumeroRecu();

            java.util.Set<String> emails = new java.util.LinkedHashSet<>();
            for (Parent parent : parents) {
                if (parent.getProfil() == null) continue;
                Utilisateur compte = parent.getProfil().getUtilisateur();
                if (compte != null) {
                    try {
                        notificationService.envoyerNotification(
                                Notification.builder()
                                        .titre("Paiement reçu")
                                        .contenu(String.format("Paiement de %s enregistré pour %s. Reçu N° %s (envoyé par e-mail si une adresse est renseignée).",
                                                montant, nomEleve, numeroRecu))
                                        .build(),
                                expediteurId, compte.getId());
                    } catch (Exception e) {
                        log.warn("Notification de paiement non envoyée au parent {} : {}", parent.getId(), e.getMessage());
                    }
                }
                String email = parent.getProfil().getEmail();
                if ((email == null || email.isBlank()) && compte != null) email = compte.getEmail();
                if (email != null && !email.isBlank()) emails.add(email.trim());
            }
            if (emails.isEmpty()) {
                return;
            }

            byte[] pdf = recuPdfService.genererRecuPdf(numeroRecu);
            String nomEtab = etablissementNom;
            Runnable envoi = () -> {
                for (String email : emails) {
                    try {
                        emailService.sendRecuPaiementEmail(email, nomEtab, nomEleve, numeroRecu, montant, pdf);
                    } catch (Exception e) {
                        log.warn("Reçu {} non envoyé à {} : {}", numeroRecu, email, e.getMessage());
                    }
                }
            };
            // Envoi après validation de la transaction (jamais de reçu pour un paiement annulé) et hors du
            // fil de la requête pour ne pas ralentir la saisie du comptable.
            if (org.springframework.transaction.support.TransactionSynchronizationManager.isSynchronizationActive()) {
                org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(
                        new org.springframework.transaction.support.TransactionSynchronization() {
                            @Override
                            public void afterCommit() {
                                java.util.concurrent.CompletableFuture.runAsync(envoi);
                            }
                        });
            } else {
                java.util.concurrent.CompletableFuture.runAsync(envoi);
            }
        } catch (Exception e) {
            log.warn("Remise du reçu {} aux parents impossible : {}", paiement.getNumeroRecu(), e.getMessage());
        }
    }

    @Override
    public List<Paiement> listerPaiementsEleve(Long eleveId) {
        return tenantGuard.filterSameNiveau(
                tenantGuard.filterSameTenant(paiementRepository.findByEleveId(eleveId)),
                p -> p.getEleve() != null ? niveauDe(p.getEleve()) : null);
    }

    @Override
    public Optional<Paiement> trouverParNumeroRecu(String numeroRecu) {
        return paiementRepository.findByNumeroRecu(numeroRecu).filter(tenantGuard::appartientAuTenantCourant);
    }

    @Override
    @Transactional(readOnly = true)
    public com.gestionscolaire.gestion_scolaire_backend.modules.comptabilite.dto.SituationFinanciereResponse situationEleve(Long eleveId) {
        Eleve eleve = tenantGuard.requireSameTenant(eleveRepository.findById(eleveId)
                .orElseThrow(() -> new ResourceNotFoundException("Élève introuvable")));
        verifierAccesFamille(eleve);

        String devise = "FCFA";
        if (eleve.getEtablissement() != null && eleve.getEtablissement().getDevise() != null
                && !eleve.getEtablissement().getDevise().isBlank()) {
            devise = eleve.getEtablissement().getDevise();
        }

        List<Paiement> paiements = new java.util.ArrayList<>(paiementRepository.findByEleveId(eleveId));
        paiements.sort(java.util.Comparator.comparing(Paiement::getDatePaiement).reversed());

        List<FraisScolarite> frais = eleve.getClasse() == null ? new java.util.ArrayList<>()
                : new java.util.ArrayList<>(fraisScolariteRepository.findByClasseId(eleve.getClasse().getId()));
        frais.sort(java.util.Comparator.comparing(FraisScolarite::getDateEcheance));

        // Paiements rattachés à un frais précis ; le crédit libre (sans frais) couvre les échéances les plus anciennes.
        java.util.Map<Long, Double> payeParFrais = new java.util.HashMap<>();
        double libre = 0;
        for (Paiement p : paiements) {
            double m = p.getMontantPaye() != null ? p.getMontantPaye() : 0;
            if (p.getFraisScolarite() != null) payeParFrais.merge(p.getFraisScolarite().getId(), m, Double::sum);
            else libre += m;
        }

        java.time.LocalDate aujourdhui = java.time.LocalDate.now();
        List<com.gestionscolaire.gestion_scolaire_backend.modules.comptabilite.dto.SituationFinanciereResponse.LigneFrais> lignes = new java.util.ArrayList<>();
        double totalDu = 0;
        double totalPaye = 0;
        for (FraisScolarite f : frais) {
            double paye = payeParFrais.getOrDefault(f.getId(), 0.0);
            double manque = Math.max(0, f.getMontant() - paye);
            double applique = Math.min(libre, manque);
            paye += applique;
            libre -= applique;
            double reste = Math.max(0, f.getMontant() - paye);
            String statut = reste <= 0 ? "PAYE"
                    : f.getDateEcheance().isBefore(aujourdhui) ? "EN_RETARD"
                    : paye > 0 ? "PARTIEL" : "A_PAYER";
            double payeRetenu = Math.min(paye, f.getMontant());
            totalDu += f.getMontant();
            totalPaye += payeRetenu;
            lignes.add(new com.gestionscolaire.gestion_scolaire_backend.modules.comptabilite.dto.SituationFinanciereResponse.LigneFrais(
                    f.getId(), f.getTitre(), typeDeFrais(f.getTitre()), f.getMontant(), payeRetenu,
                    reste, f.getDateEcheance(), statut));
        }

        List<com.gestionscolaire.gestion_scolaire_backend.modules.comptabilite.dto.SituationFinanciereResponse.PaiementRecu> recus = paiements.stream()
                .map(p -> new com.gestionscolaire.gestion_scolaire_backend.modules.comptabilite.dto.SituationFinanciereResponse.PaiementRecu(
                        p.getId(), p.getNumeroRecu(), p.getMontantPaye() != null ? p.getMontantPaye() : 0,
                        p.getDatePaiement(), p.getModePaiement(),
                        p.getFraisScolarite() != null ? p.getFraisScolarite().getTitre() : "Paiement"))
                .toList();

        double reste = Math.max(0, totalDu - totalPaye);
        // Tant que l'école n'a enregistré que l'inscription, le reste de la scolarité (mensualités,
        // tranches…) n'est pas connu : on ne peut donc pas déclarer l'année « toute payée ».
        boolean scolariteDefinie = lignes.stream().anyMatch(l -> !"INSCRIPTION".equals(l.type()));
        return new com.gestionscolaire.gestion_scolaire_backend.modules.comptabilite.dto.SituationFinanciereResponse(
                devise, totalDu, totalPaye, reste, frais.isEmpty(), scolariteDefinie && reste <= 0,
                scolariteDefinie, Math.max(0, libre), lignes, recus);
    }

    /** Un parent ne voit que ses enfants, un élève que lui-même ; direction et comptabilité voient tout l'établissement. */
    private void verifierAccesFamille(Eleve eleve) {
        Utilisateur courant;
        try {
            courant = com.gestionscolaire.gestion_scolaire_backend.core.security.SecurityUtils.getCurrentUser().getUtilisateur();
        } catch (Exception e) {
            return;
        }
        String role = courant.getRole() != null ? courant.getRole().getNom() : "";
        boolean concerne;
        if ("PARENT".equalsIgnoreCase(role)) {
            concerne = estCompteDe(eleve.getParent(), courant) || estCompteDe(eleve.getParentSecondaire(), courant);
        } else if ("ELEVE".equalsIgnoreCase(role)) {
            concerne = eleve.getProfil() != null && eleve.getProfil().getUtilisateur() != null
                    && courant.getId().equals(eleve.getProfil().getUtilisateur().getId());
        } else {
            return;
        }
        if (!concerne) {
            throw new ResourceNotFoundException("Élève introuvable");
        }
    }

    private static boolean estCompteDe(Parent parent, Utilisateur courant) {
        return parent != null && parent.getProfil() != null && parent.getProfil().getUtilisateur() != null
                && courant.getId().equals(parent.getProfil().getUtilisateur().getId());
    }

    /** Classe un frais selon son intitulé : inscription, mensualité (ou nom de mois) ou autre. */
    private static String typeDeFrais(String titre) {
        String t = titre == null ? "" : java.text.Normalizer.normalize(titre, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").toLowerCase();
        if (t.contains("inscri")) return "INSCRIPTION";
        if (t.contains("mensu") || t.contains("mois")
                || t.matches(".*(janvier|fevrier|mars|avril|mai|juin|juillet|aout|septembre|octobre|novembre|decembre).*")) {
            return "MENSUALITE";
        }
        return "AUTRE";
    }

    @Override
    public Double calculerSoldeRestantEleve(Long eleveId) {
        Eleve eleve = tenantGuard.requireSameTenant(eleveRepository.findById(eleveId)
                .orElseThrow(() -> new ResourceNotFoundException("Élève introuvable")));

        if (eleve.getClasse() == null) {
            return 0.0;
        }

        // Somme des frais de scolarité de sa classe
        List<FraisScolarite> fraisClasse = fraisScolariteRepository.findByClasseId(eleve.getClasse().getId());
        double totalFrais = fraisClasse.stream().mapToDouble(FraisScolarite::getMontant).sum();

        // Somme des paiements déjà effectués par cet élève
        List<Paiement> paiementsEleve = paiementRepository.findByEleveId(eleveId);
        double totalPaye = paiementsEleve.stream().mapToDouble(Paiement::getMontantPaye).sum();

        return totalFrais - totalPaye;
    }

    @Override
    public List<RetardPaiementResponse> listerRetardsPaiement() {
        java.time.LocalDate aujourdHui = java.time.LocalDate.now();

        List<Eleve> eleves = tenantGuard.crossTenant()
                ? eleveRepository.findAll()
                : eleveRepository.findByEtablissementId(tenantGuard.requireEtablissementId());
        eleves = tenantGuard.filterSameNiveau(eleves, this::niveauDe);

        List<RetardPaiementResponse> retards = new java.util.ArrayList<>();

        for (Eleve eleve : eleves) {
            if (eleve.getClasse() == null || !"ACTIF".equalsIgnoreCase(eleve.getStatut())) {
                continue;
            }

            List<FraisScolarite> fraisEchus = fraisScolariteRepository.findByClasseId(eleve.getClasse().getId())
                    .stream()
                    .filter(f -> f.getDateEcheance() != null && f.getDateEcheance().isBefore(aujourdHui))
                    .toList();
            if (fraisEchus.isEmpty()) continue;

            double montantEchu = fraisEchus.stream().mapToDouble(FraisScolarite::getMontant).sum();
            double totalPaye = paiementRepository.findByEleveId(eleve.getId()).stream()
                    .mapToDouble(Paiement::getMontantPaye).sum();
            double resteDu = montantEchu - totalPaye;
            if (resteDu <= 0.009) continue; // couvert malgré l'échéance dépassée

            java.time.LocalDate echeanceLaPlusAncienne = fraisEchus.stream()
                    .map(FraisScolarite::getDateEcheance)
                    .min(java.time.LocalDate::compareTo)
                    .orElse(aujourdHui);
            long joursRetard = java.time.temporal.ChronoUnit.DAYS.between(echeanceLaPlusAncienne, aujourdHui);
            if (joursRetard < 1) continue; // retard = échéance dépassée d'au moins 1 jour plein

            Parent parent = eleve.getParent();
            Profil profilParent = parent != null ? parent.getProfil() : null;

            retards.add(RetardPaiementResponse.builder()
                    .eleveId(eleve.getId())
                    .eleveNom(eleve.getProfil() != null ? eleve.getProfil().getNom() : null)
                    .elevePrenom(eleve.getProfil() != null ? eleve.getProfil().getPrenom() : null)
                    .matricule(eleve.getMatricule())
                    .classeId(eleve.getClasse().getId())
                    .classeNom(eleve.getClasse().getNom())
                    .parentNom(profilParent != null ? profilParent.getNom() : null)
                    .parentPrenom(profilParent != null ? profilParent.getPrenom() : null)
                    .parentTelephone(profilParent != null ? profilParent.getTelephone() : null)
                    .montantDu(resteDu)
                    .echeanceLaPlusAncienne(echeanceLaPlusAncienne)
                    .joursRetard(joursRetard)
                    .build());
        }

        retards.sort((a, b) -> Long.compare(b.getJoursRetard(), a.getJoursRetard()));
        return retards;
    }
}


