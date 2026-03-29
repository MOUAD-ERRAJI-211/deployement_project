package orthoproconnect.service.impl;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import orthoproconnect.service.QrCodeService;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.UUID;

@Service
public class QrCodeServiceImpl implements QrCodeService {

    private static final Logger logger = LoggerFactory.getLogger(QrCodeServiceImpl.class);

    @Value("${app.qrcode-dir:uploads/qr-codes}")
    private String qrCodeStoragePath;

    @Value("${qrcode.salt:Ort3ISPITS!Secure}")
    private String qrCodeSalt;

    @Override
    public byte[] generateQrCode(String data, int width, int height) {
        try {
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(data, BarcodeFormat.QR_CODE, width, height);

            ByteArrayOutputStream pngOutputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", pngOutputStream);

            return pngOutputStream.toByteArray();
        } catch (Exception e) {
            logger.error("Error generating QR code", e);
            throw new RuntimeException("Failed to generate QR code", e);
        }
    }

    /**
     * Saves a QR code image to disk (backward compatibility method)
     *
     * @param qrCode The QR code image as a byte array
     * @param studentId The student's ID
     * @return The path to the saved QR code image
     */
    @Override
    public String saveQrCodeToDisk(byte[] qrCode, String studentId) {
        // Default to using the student ID as the name if actual name is not provided
        return saveQrCodeToDisk(qrCode, studentId, "student_" + studentId);
    }

    @Override
    public String saveQrCodeToDisk(byte[] qrCode, String studentId, String studentName) {
        try {
            // Create directory if it doesn't exist
            File directory = new File(qrCodeStoragePath);
            if (!directory.exists()) {
                boolean created = directory.mkdirs();
                if (!created) {
                    throw new RuntimeException("Failed to create QR code directory: " + qrCodeStoragePath);
                }
            }

            // Sanitize student name for use in filename (remove special characters, replace spaces with underscores)
            String sanitizedName = studentName.replaceAll("[^a-zA-Z0-9\\-_.]", "_")
                    .replaceAll("\\s+", "_")
                    .toLowerCase();

            // Generate filename with student name and timestamp
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
            String filename = String.format("%s_%s_%s.png", sanitizedName, studentId, timestamp);
            Path filePath = Paths.get(qrCodeStoragePath, filename);

            // Save QR code to file
            try (FileOutputStream fos = new FileOutputStream(filePath.toFile())) {
                fos.write(qrCode);
            }

            // Set appropriate file permissions if running on Unix-like system
            try {
                Files.setPosixFilePermissions(filePath, java.nio.file.attribute.PosixFilePermissions.fromString("rw-r-----"));
            } catch (UnsupportedOperationException e) {
                // Windows or other non-POSIX filesystem, ignore
                logger.debug("POSIX file permissions not supported on this system");
            }

            // Return the full path to the saved file
            return filePath.toString();
        } catch (IOException e) {
            logger.error("Error saving QR code to disk", e);
            throw new RuntimeException("Failed to save QR code to disk", e);
        }
    }

    @Override
    public String generateQrCodeData(Long studentId) {
        // Generate a secure random token
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[16];
        random.nextBytes(bytes);
        String randomToken = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        // Combine student ID, salt, and random token
        String data = String.format("STUDENT-%d-%s-%s", studentId, qrCodeSalt, randomToken);

        // Debug log
        System.out.println("Generated QR code data: " + "QR-" + UUID.nameUUIDFromBytes(data.getBytes()).toString());

        // Create a UUID that encodes this information securely
        return "QR-" + UUID.nameUUIDFromBytes(data.getBytes()).toString();
    }
}