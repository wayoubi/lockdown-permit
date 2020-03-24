package modee.gov.jo;


import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.oned.EAN13Writer;
import com.google.zxing.pdf417.PDF417Writer;
import com.google.zxing.qrcode.QRCodeWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.*;
import java.security.cert.CertificateException;
import java.util.Base64;

/**
 * Hello world!
 *
 */
public class SingingApp {

    /**
     *
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(SingingApp.class);

    /**
     *
     * @param fileName
     * @param keystoreFormat
     * @param keypairAlias
     * @param keystorePassword
     * @param privateKeyPassword
     * @return
     */
    public PrivateKey loadPrivateKey(String fileName, String keystoreFormat, String keypairAlias, String keystorePassword, String privateKeyPassword) {
        PrivateKey privateKey = null;
        try {
            KeyStore keyStore  = KeyStore.getInstance(keystoreFormat);
            keyStore.load(new FileInputStream(fileName), keystorePassword.toCharArray());
            privateKey = (PrivateKey) keyStore.getKey(keypairAlias, privateKeyPassword.toCharArray());
        } catch (KeyStoreException e) {
            LOGGER.error(e.getMessage());
        } catch (IOException e) {
            LOGGER.error(e.getMessage());
        } catch (CertificateException e) {
            LOGGER.error(e.getMessage());
        } catch (NoSuchAlgorithmException e) {
            LOGGER.error(e.getMessage());
        } catch (UnrecoverableKeyException e) {
            LOGGER.error(e.getMessage());
        }
        return privateKey;
    }

    /**
     *
     * @param barcodeText
     * @return
     * @throws Exception
     */
    public static BufferedImage generatePDF_417BarcodeImage(String barcodeText) throws Exception {
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        BitMatrix bitMatrix = qrCodeWriter.encode(barcodeText, BarcodeFormat.QR_CODE, 300, 150);
        return MatrixToImageWriter.toBufferedImage(bitMatrix);
    }

    public static void main( String[] args ) {

        SingingApp app = new SingingApp();
        PrivateKey privateKey = app.loadPrivateKey("tasree7_keystore.p12", "PKCS12", "tasree7KeyPair", "changeit", "changeit");

        //Read the Permit content from text file
        byte[] permitBytes = new byte[0];
        try {
            permitBytes = Files.readAllBytes(Paths.get("permit.txt"));
            LOGGER.info(String.format("Permit Text: %s", new String(permitBytes)));
        } catch (IOException e) {
            LOGGER.error(e.getMessage());
        }

        // Generate Permit Digest
        byte[] messageHash = new byte[0];
        try {
            MessageDigest md  = MessageDigest.getInstance("SHA-256");
            messageHash = md.digest(permitBytes);
            String encodedString = Base64.getEncoder().encodeToString(messageHash);
            LOGGER.info(String.format("Permit Encoded Hash: %s", encodedString));
        } catch (NoSuchAlgorithmException e) {
            LOGGER.error(e.getMessage());
        }

        //Sign the Permit Digest
        String encodedDigitalSignature = null;
        try {
            Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.ENCRYPT_MODE, privateKey);
            byte[] digitalSignature = cipher.doFinal(messageHash);
            encodedDigitalSignature = Base64.getEncoder().encodeToString(digitalSignature);
            LOGGER.info(String.format("Encoded Digital Signature: %s", encodedDigitalSignature));
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
        }

        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(new String(permitBytes));
        stringBuilder.append(System.lineSeparator());
        stringBuilder.append(String.format("Signature: %s", encodedDigitalSignature));
        try {
            BufferedImage bufferedImage = app.generatePDF_417BarcodeImage(stringBuilder.toString());
            File outputfile = new File("saved.png");
            ImageIO.write(bufferedImage, "png", outputfile);
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
        }
    }
}
