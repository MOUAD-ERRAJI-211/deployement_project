package orthoproconnect.service;

public interface QrCodeService {
    /**
     * Generates a QR code image
     *
     * @param data The data to encode in the QR code
     * @param width The width of the QR code image
     * @param height The height of the QR code image
     * @return The QR code image as a byte array
     */
    byte[] generateQrCode(String data, int width, int height);

    /**
     * Saves a QR code image to disk
     *
     * @param qrCode The QR code image as a byte array
     * @param studentId The student's ID
     * @return The path to the saved QR code image
     */
    String saveQrCodeToDisk(byte[] qrCode, String studentId);

    /**
     * Saves a QR code image to disk with the student's name in the filename
     *
     * @param qrCode The QR code image as a byte array
     * @param studentId The student's ID
     * @param studentName The student's name (used for the filename)
     * @return The path to the saved QR code image
     */
    String saveQrCodeToDisk(byte[] qrCode, String studentId, String studentName);

    /**
     * Generates unique QR code data for a student
     *
     * @param studentId The student's ID
     * @return The generated QR code data
     */
    String generateQrCodeData(Long studentId);
}