package com.gestionscolaire.gestion_scolaire_backend.core.verification;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;

/**
 * Génère les QR codes de vérification apposés sur les documents officiels
 * (reçus, bulletins, fiche d'abonnement) : chaque QR pointe vers une page
 * publique affichant la donnée réelle enregistrée en base, pour qu'une copie
 * modifiée soit détectable au scan.
 */
@Service
public class QrCodeService {

    /** PNG du QR encodant {@code contenu} (typiquement une URL de vérification). */
    public byte[] genererPng(String contenu, int taillePx) {
        try {
            QRCodeWriter writer = new QRCodeWriter();
            Map<EncodeHintType, Object> hints = Map.of(
                    EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M,
                    EncodeHintType.MARGIN, 1);
            BitMatrix matrix = writer.encode(contenu, BarcodeFormat.QR_CODE, taillePx, taillePx, hints);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", out);
            return out.toByteArray();
        } catch (WriterException | IOException e) {
            throw new IllegalStateException("Génération du QR de vérification impossible", e);
        }
    }
}
