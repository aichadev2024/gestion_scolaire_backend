package com.gestionscolaire.gestion_scolaire_backend.modules.iam.services;

import com.gestionscolaire.gestion_scolaire_backend.core.exceptions.BadRequestException;
import com.gestionscolaire.gestion_scolaire_backend.core.exceptions.ResourceNotFoundException;
import com.gestionscolaire.gestion_scolaire_backend.modules.iam.models.Profil;
import com.gestionscolaire.gestion_scolaire_backend.modules.iam.models.Role;
import com.gestionscolaire.gestion_scolaire_backend.modules.iam.models.Utilisateur;
import com.gestionscolaire.gestion_scolaire_backend.modules.iam.repositories.ProfilRepository;
import com.gestionscolaire.gestion_scolaire_backend.modules.iam.repositories.RoleRepository;
import com.gestionscolaire.gestion_scolaire_backend.modules.iam.repositories.UtilisateurRepository;
import com.gestionscolaire.gestion_scolaire_backend.modules.iam.services.UtilisateurService;
import com.gestionscolaire.gestion_scolaire_backend.core.services.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class UtilisateurServiceImpl implements UtilisateurService {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(UtilisateurServiceImpl.class);

    @Autowired
    private UtilisateurRepository utilisateurRepository;

    @Autowired
    private ProfilRepository profilRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EmailService emailService;

    @Autowired
    private com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.repositories.ParentRepository parentRepository;

    @Autowired
    private com.gestionscolaire.gestion_scolaire_backend.core.tenancy.TenantGuard tenantGuard;

    @Autowired
    private com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.repositories.EleveRepository eleveRepository;

    @Autowired
    private com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.repositories.EnseignantRepository enseignantRepository;

    @Autowired
    private com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.repositories.ClasseRepository classeRepository;

    @Autowired
    private com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.repositories.NiveauRepository niveauRepository;

    // @Lazy : EleveService/EnseignantService dépendent de UtilisateurService → on casse le cycle.
    @Autowired
    @Lazy
    private com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.services.EleveService eleveService;

    @Autowired
    @Lazy
    private com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.services.EnseignantService enseignantService;

    @Override
    public Utilisateur inscrire(Utilisateur utilisateur, Profil profil, String nomRole) {
        Role role = roleRepository.findByNom(nomRole)
                .orElseThrow(() -> new ResourceNotFoundException("Rôle introuvable : " + nomRole));
        
        if (utilisateur.getEtablissement() == null) {
            try {
                utilisateur.setEtablissement(com.gestionscolaire.gestion_scolaire_backend.core.security.SecurityUtils.getCurrentUser().getUtilisateur().getEtablissement());
            } catch (Exception ignored) {}
        }

        if (utilisateur.getEmail() != null && utilisateur.getEmail().isBlank()) {
            utilisateur.setEmail(null);
        }
        if (profil != null && profil.getEmail() != null && profil.getEmail().isBlank()) {
            profil.setEmail(null);
        }

        String rawPassword = utilisateur.getMotDePasse();
        utilisateur.setRole(role);
        if (utilisateur.getEstPremierLogin() == null) {
            utilisateur.setEstPremierLogin(true);
        }
        utilisateur.setMotDePasse(passwordEncoder.encode(utilisateur.getMotDePasse()));
        Utilisateur savedUser = utilisateurRepository.save(utilisateur);

        if (profil != null) {
            if (profil.getEmail() == null || profil.getEmail().isBlank()) {
                profil.setEmail(savedUser.getEmail());
            }
            profil.setUtilisateur(savedUser);
            Profil savedProfil = profilRepository.save(profil);

        // Si le rôle créé est PARENT, on crée automatiquement l'enregistrement dans la table parents
        if ("PARENT".equalsIgnoreCase(nomRole)) {
            if (parentRepository.findByProfilUtilisateurId(savedUser.getId()).isEmpty()) {
                parentRepository.save(com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.models.Parent.builder()
                        .profil(savedProfil)
                        .build());
            }
        }

        // Un compte créé ici avec le rôle ENSEIGNANT (page « Comptes utilisateurs ») n'avait
        // jusqu'ici aucune fiche enseignant associée — il n'apparaissait pas sur la page
        // Enseignants et ne pouvait être assigné à aucune classe/matière. On crée désormais la
        // fiche automatiquement, comme le fait déjà « Enseignants → Ajouter » (matricule généré).
        if ("ENSEIGNANT".equalsIgnoreCase(nomRole)) {
            creerFicheEnseignantSiAbsente(savedUser, savedProfil);
        }
        }

        // Envoi automatique de l'email de bienvenue Brevo (si l'utilisateur possède un e-mail)
        if (savedUser.getEmail() != null && !savedUser.getEmail().isBlank()) {
            try {
                logger.info("📧 [ENVOI BIENVENUE] Envoi des identifiants au compte [{}] ({}) à [{}]", savedUser.getUsername(), nomRole, savedUser.getEmail());
                emailService.sendWelcomeEmail(savedUser, rawPassword);
            } catch (Exception e) {
                logger.error("❌ Erreur lors de l'envoi de l'email de bienvenue à {}: {}", savedUser.getEmail(), e.getMessage());
            }
        } else {
            logger.info("ℹ️ Compte [{}] ({}) créé sans adresse e-mail. Les identifiants doivent être transmis manuellement par l'administration.", savedUser.getUsername(), nomRole);
        }

        return savedUser;
    }

    @org.springframework.context.event.EventListener(org.springframework.boot.context.event.ApplicationReadyEvent.class)
    public void syncParentsOnStartup() {
        try {
            List<Utilisateur> parentsUsers = utilisateurRepository.findAll().stream()
                    .filter(u -> u.getRole() != null && "PARENT".equalsIgnoreCase(u.getRole().getNom()))
                    .toList();

            for (Utilisateur u : parentsUsers) {
                if (parentRepository.findByProfilUtilisateurId(u.getId()).isEmpty()) {
                    Profil p = profilRepository.findByUtilisateurId(u.getId()).orElse(null);
                    if (p != null) {
                        parentRepository.save(com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.models.Parent.builder()
                                .profil(p)
                                .build());
                    }
                }
            }
        } catch (Exception ignored) {}
    }

    /**
     * Répare au démarrage les comptes ENSEIGNANT créés AVANT ce correctif (via « Comptes
     * utilisateurs », sans passer par « Enseignants ») et qui n'ont donc aucune fiche —
     * invisibles sur la page Enseignants, impossibles à assigner à une classe/matière.
     */
    @org.springframework.context.event.EventListener(org.springframework.boot.context.event.ApplicationReadyEvent.class)
    public void syncEnseignantsOnStartup() {
        try {
            List<Utilisateur> enseignantUsers = utilisateurRepository.findAll().stream()
                    .filter(u -> u.getRole() != null && "ENSEIGNANT".equalsIgnoreCase(u.getRole().getNom()))
                    .toList();

            for (Utilisateur u : enseignantUsers) {
                Profil p = profilRepository.findByUtilisateurId(u.getId()).orElse(null);
                if (p != null) {
                    creerFicheEnseignantSiAbsente(u, p);
                }
            }
        } catch (Exception ignored) {}
    }

    /** Matricule généré selon le même format que « Enseignants → Ajouter » (T-GEN-0001…). */
    private void creerFicheEnseignantSiAbsente(Utilisateur utilisateur, Profil profil) {
        if (enseignantRepository.findByProfilUtilisateurId(utilisateur.getId()).isPresent()) {
            return;
        }
        String seq = String.format("%04d", enseignantRepository.count() + 1);
        String matricule = "T-GEN-" + seq;
        while (enseignantRepository.findByMatricule(matricule).isPresent()) {
            seq = String.format("%04d", Integer.parseInt(seq) + 1);
            matricule = "T-GEN-" + seq;
        }
        enseignantRepository.save(com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.models.Enseignant.builder()
                .profil(profil)
                .matricule(matricule)
                .etablissement(utilisateur.getEtablissement())
                .build());
    }

    @Override
    public Utilisateur inscrirePremierAdmin(Utilisateur utilisateur, Profil profil) {
        if (utilisateurRepository.existsByRoleNom("DIRECTEUR")) {
            throw new BadRequestException("Un directeur existe déjà pour cet établissement. La création d'autres comptes doit se faire par un directeur connecté.");
        }
        return inscrire(utilisateur, profil, "DIRECTEUR");
    }

    @Override
    public Utilisateur inscrireSuperAdmin(Utilisateur utilisateur, Profil profil) {
        if (utilisateurRepository.existsByRoleNom("SUPER_ADMIN")) {
            throw new BadRequestException("Un compte Super-Admin existe déjà sur la plateforme Netaa. La création de compte Super-Admin est verrouillée.");
        }
        utilisateur.setEtablissement(null); // Le SuperAdmin doit impérativement avoir etablissement_id = null
        return inscrire(utilisateur, profil, "SUPER_ADMIN");
    }

    @Override
    public Optional<Utilisateur> trouverParEmail(String email) {
        return utilisateurRepository.findByEmail(email);
    }

    @Override
    public List<Utilisateur> listerTous() {
        if (tenantGuard.crossTenant()) {
            return utilisateurRepository.findAll();
        }
        List<Utilisateur> comptes = utilisateurRepository.findByEtablissementId(tenantGuard.requireEtablissementId());
        Integer restriction = tenantGuard.niveauSuperviseId();
        if (restriction == null) {
            return comptes;
        }
        return comptes.stream().filter(u -> estDuNiveau(u, restriction)).toList();
    }

    /**
     * Un directeur/secrétaire restreint à un niveau ne voit que les comptes de ce même niveau.
     * Les comptes eux-mêmes marqués {@code niveauSupervise} (directeur, secrétaire de niveau...)
     * se comparent directement ; un enseignant n'a pas ce champ — sa visibilité se déduit des
     * classes dont il est professeur principal.
     */
    private boolean estDuNiveau(Utilisateur u, Integer niveauId) {
        if (u.getNiveauSupervise() != null) {
            return niveauId.equals(u.getNiveauSupervise().getId());
        }
        String roleNom = u.getRole() != null ? u.getRole().getNom() : "";
        if ("ENSEIGNANT".equalsIgnoreCase(roleNom)) {
            return enseignantRepository.findByProfilUtilisateurId(u.getId())
                    .map(ens -> classeRepository.findByEnseignantPrincipalId(ens.getId()).stream()
                            .anyMatch(c -> c.getNiveau() != null && niveauId.equals(c.getNiveau().getId())))
                    .orElse(false);
        }
        // Compte transverse non restreint (comptable, secrétariat général...) : invisible pour
        // un compte restreint — évite qu'un directeur de niveau ne tombe dessus par surprise.
        return false;
    }

    @Override
    public void modifierStatut(Long id, boolean estActif) {
        Utilisateur utilisateur = tenantGuard.requireSameTenant(utilisateurRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable")));
        utilisateur.setEstActif(estActif);
        utilisateurRepository.save(utilisateur);
    }

    @Override
    public Utilisateur modifierUtilisateur(Long id, Utilisateur details, Profil profilDetails, String nomRole, Integer niveauSuperviseId) {
        Utilisateur utilisateur = tenantGuard.requireSameTenant(utilisateurRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable ID : " + id)));

        if (details.getUsername() != null && !details.getUsername().isBlank()) {
            utilisateur.setUsername(details.getUsername().trim());
        }
        if (details.getEmail() != null) {
            utilisateur.setEmail(details.getEmail().isBlank() ? null : details.getEmail().trim());
        }

        if (details.getMotDePasse() != null && !details.getMotDePasse().isBlank()) {
            utilisateur.setMotDePasse(passwordEncoder.encode(details.getMotDePasse()));
        }

        if (nomRole != null && !nomRole.isBlank()) {
            Role role = roleRepository.findByNom(nomRole)
                    .orElseThrow(() -> new ResourceNotFoundException("Rôle introuvable : " + nomRole));
            utilisateur.setRole(role);
        }

        utilisateur.setNiveauSupervise(niveauSuperviseId != null
                ? niveauRepository.findById(niveauSuperviseId)
                        .orElseThrow(() -> new ResourceNotFoundException("Niveau introuvable"))
                : null);

        Utilisateur savedUser = utilisateurRepository.save(utilisateur);

        Profil profil = profilRepository.findByUtilisateurId(id).orElseGet(() -> Profil.builder().utilisateur(savedUser).build());
        if (profilDetails != null) {
            profil.setPrenom(profilDetails.getPrenom());
            profil.setNom(profilDetails.getNom());
            if (profilDetails.getTelephone() != null) profil.setTelephone(profilDetails.getTelephone());
            if (profilDetails.getGenre() != null) profil.setGenre(profilDetails.getGenre());
            if (profilDetails.getAdresse() != null) profil.setAdresse(profilDetails.getAdresse());
            profil.setEmail(savedUser.getEmail());
            profilRepository.save(profil);
        }

        return savedUser;
    }

    @Override
    public void supprimerUtilisateur(Long id) {
        Utilisateur utilisateur = tenantGuard.requireSameTenant(utilisateurRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable ID : " + id)));

        // Compte rattaché à une fiche élève / enseignant : supprimer d'abord la
        // fiche métier ET ses dépendances (notes, bulletins, présences, paiements
        // pour l'élève ; rattachements de classes pour l'enseignant), sinon les
        // contraintes de clé étrangère bloquent la suppression du compte.
        eleveRepository.findByProfilUtilisateurId(id)
                .ifPresent(e -> eleveService.supprimerEleve(e.getId()));
        enseignantRepository.findByProfilUtilisateurId(id)
                .ifPresent(en -> enseignantService.supprimerEnseignant(en.getId()));

        // Fiche parent éventuelle
        try {
            parentRepository.findByProfilUtilisateurId(id).ifPresent(p -> parentRepository.delete(p));
        } catch (Exception ignored) {}

        // Profil
        try {
            profilRepository.findByUtilisateurId(id).ifPresent(p -> profilRepository.delete(p));
        } catch (Exception ignored) {}

        utilisateurRepository.delete(utilisateur);
    }

    @Override
    public Utilisateur nommerDirecteur(Long id, Integer niveauId) {
        if (niveauId == null) {
            throw new BadRequestException("Le niveau à diriger est obligatoire (chaque directeur est rattaché à un seul niveau).");
        }
        Utilisateur nouveauDirecteur = tenantGuard.requireSameTenant(utilisateurRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable ID : " + id)));

        if (nouveauDirecteur.getEtablissement() == null) {
            throw new BadRequestException("Cet utilisateur n'est rattaché à aucun établissement.");
        }
        com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.models.Niveau niveau = niveauRepository.findById(niveauId)
                .orElseThrow(() -> new ResourceNotFoundException("Niveau introuvable"));

        boolean dejaDirecteurDeCeNiveau = nouveauDirecteur.getRole() != null
                && "DIRECTEUR".equalsIgnoreCase(nouveauDirecteur.getRole().getNom())
                && nouveauDirecteur.getNiveauSupervise() != null
                && niveauId.equals(nouveauDirecteur.getNiveauSupervise().getId());
        if (dejaDirecteurDeCeNiveau) {
            throw new BadRequestException("Cet utilisateur est déjà directeur de ce niveau.");
        }

        Role roleDirecteur = roleRepository.findByNom("DIRECTEUR")
                .orElseThrow(() -> new ResourceNotFoundException("Rôle introuvable : DIRECTEUR"));
        Role roleSecretaire = roleRepository.findByNom("SECRETAIRE")
                .orElseThrow(() -> new ResourceNotFoundException("Rôle introuvable : SECRETAIRE"));

        // Un niveau n'a qu'un directeur actif à la fois — mais les directeurs des AUTRES niveaux
        // (crèche, primaire, lycée...) restent en place : la séparation par niveau est le but même
        // de cette fonctionnalité. L'ancien titulaire de CE niveau redevient Secrétaire (garde son accès).
        utilisateurRepository.findByEtablissementId(nouveauDirecteur.getEtablissement().getId()).stream()
                .filter(u -> u.getRole() != null && "DIRECTEUR".equalsIgnoreCase(u.getRole().getNom()))
                .filter(u -> u.getNiveauSupervise() != null && niveauId.equals(u.getNiveauSupervise().getId()))
                .forEach(u -> {
                    u.setRole(roleSecretaire);
                    utilisateurRepository.save(u);
                });

        nouveauDirecteur.setRole(roleDirecteur);
        nouveauDirecteur.setNiveauSupervise(niveau);
        return utilisateurRepository.save(nouveauDirecteur);
    }
}


