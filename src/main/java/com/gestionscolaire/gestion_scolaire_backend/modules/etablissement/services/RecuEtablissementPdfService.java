package com.gestionscolaire.gestion_scolaire_backend.modules.etablissement.services;

import com.gestionscolaire.gestion_scolaire_backend.core.exceptions.ResourceNotFoundException;
import com.gestionscolaire.gestion_scolaire_backend.modules.etablissement.models.Etablissement;
import com.gestionscolaire.gestion_scolaire_backend.modules.etablissement.repositories.EtablissementRepository;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;

@Service
public class RecuEtablissementPdfService {

    private final EtablissementRepository etablissementRepository;

    public RecuEtablissementPdfService(EtablissementRepository etablissementRepository) {
        this.etablissementRepository = etablissementRepository;
    }

    public byte[] genererRecuAbonnementPdf(Long etablissementId) {
        Etablissement etab = etablissementRepository.findById(etablissementId)
                .orElseThrow(() -> new ResourceNotFoundException("Établissement introuvable ID : " + etablissementId));

        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Document document = new Document();
            PdfWriter.getInstance(document, outputStream);
            document.open();

            Font brandFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 22, new Color(27, 54, 93));
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, new Color(217, 119, 6));
            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, new Color(27, 54, 93));
            Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.DARK_GRAY);
            Font boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.BLACK);

            // En-tête de Marque
            Paragraph brand = new Paragraph("NETAA ÉCOLE", brandFont);
            brand.setAlignment(Element.ALIGN_CENTER);
            document.add(brand);

            Paragraph subtitle = new Paragraph("Plateforme de Gestion Scolaire Numérique — République du Mali", normalFont);
            subtitle.setAlignment(Element.ALIGN_CENTER);
            document.add(subtitle);

            document.add(new Paragraph(" "));
            
            // Titre du Document
            Paragraph docTitle = new Paragraph("ATTESTATION D'INSCRIPTION & REÇU DE PAIEMENT", titleFont);
            docTitle.setAlignment(Element.ALIGN_CENTER);
            document.add(docTitle);

            document.add(new Paragraph(" "));

            // Informations Référence & Date
            String recuRef = "RECU-NETAA-" + java.time.Year.now().getValue() + "-" + etab.getCode().toUpperCase();
            String dateFormatted = etab.getDateCreation() != null 
                    ? etab.getDateCreation().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
                    : java.time.LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));

            PdfPTable infoHeaderTable = new PdfPTable(2);
            infoHeaderTable.setWidthPercentage(100);
            
            PdfPCell cellRef = new PdfPCell(new Phrase("Référence N° : " + recuRef, boldFont));
            cellRef.setBorder(PdfPCell.NO_BORDER);
            infoHeaderTable.addCell(cellRef);

            PdfPCell cellDate = new PdfPCell(new Phrase("Date d'émission : " + dateFormatted, normalFont));
            cellDate.setHorizontalAlignment(Element.ALIGN_RIGHT);
            cellDate.setBorder(PdfPCell.NO_BORDER);
            infoHeaderTable.addCell(cellDate);

            document.add(infoHeaderTable);
            document.add(new Paragraph(" "));

            // Tableau Détails de l'Établissement Client
            PdfPTable etabTable = new PdfPTable(2);
            etabTable.setWidthPercentage(100);
            etabTable.setSpacingBefore(10f);

            ajouterLigneHeader(etabTable, "INFORMATIONS ÉTABLISSEMENT", "DÉTAILS COMPTE ADMIN", headerFont);
            ajouterLigneDouble(etabTable, "Nom : " + etab.getNom(), "Code Système : " + etab.getCode(), normalFont);
            ajouterLigneDouble(etabTable, "Téléphone : " + (etab.getTelephone() != null ? etab.getTelephone() : "N/A"), "Statut : " + etab.getStatut(), normalFont);
            ajouterLigneDouble(etabTable, "Adresse : " + (etab.getAdresse() != null ? etab.getAdresse() : "N/A"), "Email Contact : " + (etab.getEmailContact() != null ? etab.getEmailContact() : "N/A"), normalFont);

            document.add(etabTable);
            document.add(new Paragraph(" "));

            // Tableau Récapitulatif de l'Abonnement Souscrit
            PdfPTable subTable = new PdfPTable(2);
            subTable.setWidthPercentage(100);
            
            ajouterLigneHeader(subTable, "SERVICE / ABONNEMENT SOUSCRIT", "DÉTAILS FINANCIERS", headerFont);
            
            String plan = etab.getPlanTarifaire() != null ? etab.getPlanTarifaire() : "STANDARD";
            String montant = obtenirMontantParPlan(plan);
            String dateExp = etab.getDateExpirationAbonnement() != null 
                    ? etab.getDateExpirationAbonnement().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                    : "1 an à compter de l'activation";

            ajouterLigne(subTable, "Licence Logicielle", "Plateforme Netaa École (Plan " + plan + ")", headerFont, normalFont);
            ajouterLigne(subTable, "Durée de Validité", "1 An (Jusqu'au " + dateExp + ")", headerFont, normalFont);
            ajouterLigne(subTable, "Montant Total Réglé", montant, headerFont, boldFont);
            ajouterLigne(subTable, "Mode de Règlement", "Paiement / Validation Directe Super-Admin", headerFont, normalFont);
            ajouterLigne(subTable, "Statut du Paiement", "PAYÉ & VALIDÉ (ACQUITTÉ)", headerFont, boldFont);

            document.add(subTable);

            document.add(new Paragraph(" "));
            document.add(new Paragraph(" "));

            // Section Cachet & Signature
            PdfPTable signTable = new PdfPTable(2);
            signTable.setWidthPercentage(100);

            PdfPCell cellClient = new PdfPCell(new Phrase("Pour l'Établissement (Le Directeur)\n\n\n_________________________", normalFont));
            cellClient.setBorder(PdfPCell.NO_BORDER);
            signTable.addCell(cellClient);

            PdfPCell cellAdmin = new PdfPCell(new Phrase("Pour Netaa École (La Direction)\n\n\n[ Cachet & Signature Électronique ]", headerFont));
            cellAdmin.setHorizontalAlignment(Element.ALIGN_RIGHT);
            cellAdmin.setBorder(PdfPCell.NO_BORDER);
            signTable.addCell(cellAdmin);

            document.add(signTable);

            document.add(new Paragraph(" "));
            document.add(new Paragraph(" "));
            Paragraph footer = new Paragraph("Document officiel généré automatiquement par Netaa École — Valable pour valoir ce que de droit.", normalFont);
            footer.setAlignment(Element.ALIGN_CENTER);
            document.add(footer);

            document.close();
            return outputStream.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la génération du reçu PDF pour l'établissement : " + e.getMessage(), e);
        }
    }

    private String obtenirMontantParPlan(String plan) {
        if ("PREMIUM".equalsIgnoreCase(plan)) return "500 000 FCFA";
        if ("PRO".equalsIgnoreCase(plan) || "ENTERPRISE".equalsIgnoreCase(plan)) return "1 000 000 FCFA";
        return "250 000 FCFA";
    }

    private void ajouterLigneHeader(PdfPTable table, String col1, String col2, Font font) {
        PdfPCell cell1 = new PdfPCell(new Phrase(col1, font));
        cell1.setBackgroundColor(new Color(241, 245, 249));
        cell1.setPadding(6f);
        table.addCell(cell1);

        PdfPCell cell2 = new PdfPCell(new Phrase(col2, font));
        cell2.setBackgroundColor(new Color(241, 245, 249));
        cell2.setPadding(6f);
        table.addCell(cell2);
    }

    private void ajouterLigneDouble(PdfPTable table, String col1, String col2, Font font) {
        PdfPCell cell1 = new PdfPCell(new Phrase(col1, font));
        cell1.setPadding(5f);
        table.addCell(cell1);

        PdfPCell cell2 = new PdfPCell(new Phrase(col2, font));
        cell2.setPadding(5f);
        table.addCell(cell2);
    }

    private void ajouterLigne(PdfPTable table, String label, String value, Font labelFont, Font valueFont) {
        PdfPCell cell1 = new PdfPCell(new Phrase(label, labelFont));
        cell1.setPadding(5f);
        table.addCell(cell1);

        PdfPCell cell2 = new PdfPCell(new Phrase(value, valueFont));
        cell2.setPadding(5f);
        table.addCell(cell2);
    }
}
