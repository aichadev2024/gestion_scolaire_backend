package com.gestionscolaire.gestion_scolaire_backend.modules.scolarite.services;

import com.gestionscolaire.gestion_scolaire_backend.core.exceptions.BadRequestException;
import com.gestionscolaire.gestion_scolaire_backend.core.exceptions.ResourceNotFoundException;
import com.gestionscolaire.gestion_scolaire_backend.core.verification.QrCodeService;
import com.gestionscolaire.gestion_scolaire_backend.modules.comptabilite.models.Paiement;
import com.gestionscolaire.gestion_scolaire_backend.modules.comptabilite.repositories.PaiementRepository;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Image;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;

@Service
public class RecuPdfService {

    private final PaiementRepository paiementRepository;
    private final com.gestionscolaire.gestion_scolaire_backend.core.tenancy.TenantGuard tenantGuard;
    private final QrCodeService qrCodeService;

    @Value("${app.frontend-url:http://localhost:3000}")
    private String frontendUrl;

    public RecuPdfService(PaiementRepository paiementRepository,
                          com.gestionscolaire.gestion_scolaire_backend.core.tenancy.TenantGuard tenantGuard,
                          QrCodeService qrCodeService) {
        this.paiementRepository = paiementRepository;
        this.tenantGuard = tenantGuard;
        this.qrCodeService = qrCodeService;
    }

    public byte[] genererRecuPdf(String numeroRecu) {
        Paiement paiement = tenantGuard.requireSameTenant(paiementRepository.findByNumeroRecu(numeroRecu)
                .orElseThrow(() -> new ResourceNotFoundException("Paiement introuvable pour le reçu : " + numeroRecu)));

        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Document document = new Document();
            PdfWriter.getInstance(document, outputStream);
            document.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
            Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 11);

            document.add(new Paragraph("REÇU DE PAIEMENT", titleFont));
            document.add(new Paragraph(" "));
            String etablissementNom = "Gestion Scolaire";
            if (paiement.getEleve() != null && paiement.getEleve().getProfil() != null && 
                paiement.getEleve().getProfil().getUtilisateur() != null && 
                paiement.getEleve().getProfil().getUtilisateur().getEtablissement() != null) {
                etablissementNom = paiement.getEleve().getProfil().getUtilisateur().getEtablissement().getNom();
            }
            document.add(new Paragraph("Établissement : " + etablissementNom, normalFont));
            document.add(new Paragraph("N° Reçu : " + paiement.getNumeroRecu(), headerFont));
            document.add(new Paragraph("Date : " + paiement.getDatePaiement()
                    .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")), normalFont));
            document.add(new Paragraph(" "));

            PdfPTable table = new PdfPTable(2);
            table.setWidthPercentage(100);
            ajouterLigne(table, "Élève", nomEleve(paiement), headerFont, normalFont);
            ajouterLigne(table, "Matricule", paiement.getEleve().getMatricule(), headerFont, normalFont);
            ajouterLigne(table, "Classe", classeEleve(paiement), headerFont, normalFont);
            ajouterLigne(table, "Frais", fraisTitre(paiement), headerFont, normalFont);
            ajouterLigne(table, "Montant payé", String.format("%.2f FCFA", paiement.getMontantPaye()), headerFont, normalFont);
            ajouterLigne(table, "Mode de paiement", paiement.getModePaiement(), headerFont, normalFont);
            if (paiement.getReferenceTransaction() != null) {
                ajouterLigne(table, "Référence", paiement.getReferenceTransaction(), headerFont, normalFont);
            }
            document.add(table);

            document.add(new Paragraph(" "));
            document.add(new Paragraph("Merci pour votre paiement.", normalFont));

            // QR de vérification : la valeur affichée ici fait foi ; une copie
            // modifiée du reçu (montant, nom…) ne changera pas ce que le scan révèle.
            String urlVerification = frontendUrl + "/verify/recu/" + numeroRecu;
            Image qr = Image.getInstance(qrCodeService.genererPng(urlVerification, 240));
            qr.scaleToFit(85, 85);
            qr.setAlignment(Element.ALIGN_CENTER);
            document.add(new Paragraph(" "));
            document.add(qr);

            document.close();
            return outputStream.toByteArray();
        } catch (Exception e) {
            throw new BadRequestException("Erreur lors de la génération du reçu PDF");
        }
    }

    private void ajouterLigne(PdfPTable table, String label, String value, Font headerFont, Font normalFont) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, headerFont));
        labelCell.setBorder(PdfPCell.NO_BORDER);
        labelCell.setHorizontalAlignment(Element.ALIGN_LEFT);

        PdfPCell valueCell = new PdfPCell(new Phrase(value, normalFont));
        valueCell.setBorder(PdfPCell.NO_BORDER);
        valueCell.setHorizontalAlignment(Element.ALIGN_LEFT);

        table.addCell(labelCell);
        table.addCell(valueCell);
    }

    private String nomEleve(Paiement paiement) {
        if (paiement.getEleve().getProfil() == null) {
            return "N/A";
        }
        return paiement.getEleve().getProfil().getPrenom() + " " + paiement.getEleve().getProfil().getNom();
    }

    private String classeEleve(Paiement paiement) {
        return paiement.getEleve().getClasse() != null ? paiement.getEleve().getClasse().getNom() : "N/A";
    }

    private String fraisTitre(Paiement paiement) {
        return paiement.getFraisScolarite() != null ? paiement.getFraisScolarite().getTitre() : "Paiement général";
    }
}


