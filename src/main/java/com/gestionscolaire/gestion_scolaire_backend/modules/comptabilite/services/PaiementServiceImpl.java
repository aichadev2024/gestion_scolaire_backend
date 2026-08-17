package com.gestionscolaire.gestion_scolaire_backend.modules.comptabilite.services;

import com.gestionscolaire.gestion_scolaire_backend.core.exceptions.ResourceNotFoundException;
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

    @Override
    public Paiement enregistrerPaiement(Paiement paiement, Long eleveId, Long fraisId, Long userReceptionnaireId) {
        Eleve eleve = eleveRepository.findById(eleveId)
                .orElseThrow(() -> new ResourceNotFoundException("Élève introuvable"));

        if (fraisId != null) {
            FraisScolarite frais = fraisScolariteRepository.findById(fraisId)
                    .orElseThrow(() -> new ResourceNotFoundException("Frais de scolarité introuvables"));
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

        return paiementRepository.save(paiement);
    }

    @Override
    public List<Paiement> listerPaiementsEleve(Long eleveId) {
        return paiementRepository.findByEleveId(eleveId);
    }

    @Override
    public Optional<Paiement> trouverParNumeroRecu(String numeroRecu) {
        return paiementRepository.findByNumeroRecu(numeroRecu);
    }

    @Override
    public Double calculerSoldeRestantEleve(Long eleveId) {
        Eleve eleve = eleveRepository.findById(eleveId)
                .orElseThrow(() -> new ResourceNotFoundException("Élève introuvable"));

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
}


