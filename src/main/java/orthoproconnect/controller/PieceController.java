package orthoproconnect.controller;

import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import orthoproconnect.model.Piece;
import orthoproconnect.model.Student;
import orthoproconnect.model.Transaction;
import orthoproconnect.repository.PieceRepository;
import orthoproconnect.repository.StudentRepository;
import orthoproconnect.repository.TransactionRepository;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.util.StringUtils;
import java.nio.file.Files;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import org.springframework.util.StringUtils;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.io.IOException;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/pieces")
public class PieceController {

    @Value("${app.upload-dir:uploads}")
    private String pieceUploadDir;

    @Autowired
    private StudentRepository studentRepository;
    @Autowired
    private PieceRepository pieceRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    // GET all pieces
    @GetMapping
    public List<Piece> getAllPieces() {
        return pieceRepository.findAllOrderedById();
    }

    // GET piece by ID
    @GetMapping("/{id}")
    public ResponseEntity<?> getPieceById(@PathVariable Long id) {
        Optional<Piece> piece = pieceRepository.findById(id);
        if (piece.isPresent()) {
            return ResponseEntity.ok(piece.get());
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(createErrorResponse("PiÃ¨ce non trouvÃ©e"));
        }
    }

    // GET piece by reference
    @GetMapping("/reference/{reference}")
    public ResponseEntity<?> getPieceByReference(@PathVariable String reference) {
        Optional<Piece> piece = pieceRepository.findByReference(reference);
        if (piece.isPresent()) {
            return ResponseEntity.ok(piece.get());
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(createErrorResponse("PiÃ¨ce non trouvÃ©e avec la rÃ©fÃ©rence: " + reference));
        }
    }

    // GET pieces by category
    @GetMapping("/category/{category}")
    public List<Piece> getPiecesByCategory(@PathVariable String category) {
        return pieceRepository.findByCategory(category);
    }

    // GET low stock pieces
    @GetMapping("/low-stock")
    public List<Piece> getLowStockPieces() {
        return pieceRepository.findLowStockPieces();
    }

    // GET out of stock pieces
    @GetMapping("/out-of-stock")
    public List<Piece> getOutOfStockPieces() {
        return pieceRepository.findOutOfStockPieces();
    }

    // GET pieces by type
    @GetMapping("/type/{type}")
    public List<Piece> getPiecesByType(@PathVariable String type) {
        try {
            Piece.PieceType pieceType = Piece.PieceType.valueOf(type.toUpperCase());
            return pieceRepository.findByPieceType(pieceType);
        } catch (IllegalArgumentException e) {
            return List.of();
        }
    }

    // GET pieces by unit type
    @GetMapping("/unit-type/{unitType}")
    public List<Piece> getPiecesByUnitType(@PathVariable String unitType) {
        return pieceRepository.findByFilters(null, null, null, null, null, null, null, null, unitType);
    }

    // GET pieces by month (for Excel grouping)


    // Advanced search
    @GetMapping("/search")
    public List<Piece> searchPieces(@RequestParam(required = false) String searchTerm,
                                    @RequestParam(required = false) String category,
                                    @RequestParam(required = false) String subcategory,
                                    @RequestParam(required = false) String location,
                                    @RequestParam(required = false) Integer minStock,
                                    @RequestParam(required = false) Integer maxStock,
                                    @RequestParam(required = false) String pieceType,
                                    @RequestParam(required = false) String unitType) {

        if (searchTerm != null && !searchTerm.trim().isEmpty()) {
            return pieceRepository.findBySearchTerm(searchTerm.trim());
        }

        Piece.PieceType type = null;
        if (pieceType != null) {
            try {
                type = Piece.PieceType.valueOf(pieceType.toUpperCase());
            } catch (IllegalArgumentException e) {
                // Invalid piece type, ignore
            }
        }

        return pieceRepository.findByFilters(null, null, category, subcategory,
                location, minStock, maxStock, type, unitType);
    }

    // GET transaction history for a piece
    // Alternative approach for PieceController if you don't want to modify the Transaction model

    @GetMapping("/{id}/transactions")
    @Transactional  // This ensures lazy-loaded entities are loaded within the transaction
    public ResponseEntity<?> getPieceTransactions(@PathVariable Long id,
                                                  @RequestParam(required = false) String startDate,
                                                  @RequestParam(required = false) String endDate) {
        Optional<Piece> piece = pieceRepository.findById(id);
        if (!piece.isPresent()) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "PiÃ¨ce non trouvÃ©e");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }

        try {
            List<Transaction> transactions;

            // Load transactions
            if (startDate != null && endDate != null) {
                LocalDateTime start = LocalDateTime.parse(startDate);
                LocalDateTime end = LocalDateTime.parse(endDate);
                transactions = transactionRepository.findByPieceAndDateRange(piece.get(), start, end);
            } else {
                transactions = transactionRepository.findByPiece(piece.get());
            }

            // Create a formatted list of transactions with student data
            List<Map<String, Object>> formattedTransactions = new ArrayList<>();

            for (Transaction transaction : transactions) {
                Map<String, Object> formattedTransaction = new HashMap<>();
                formattedTransaction.put("id", transaction.getId());
                formattedTransaction.put("quantity", transaction.getQuantity());
                formattedTransaction.put("transactionType", transaction.getTransactionType());
                formattedTransaction.put("transactionDate", transaction.getTransactionDate());
                formattedTransaction.put("expectedReturnDate", transaction.getExpectedReturnDate());
                formattedTransaction.put("actualReturnDate", transaction.getActualReturnDate());
                formattedTransaction.put("notes", transaction.getNotes());
                formattedTransaction.put("authorizedBy", transaction.getAuthorizedBy());
                formattedTransaction.put("status", transaction.getStatus());
                formattedTransaction.put("returnedQuantity", transaction.getReturnedQuantity());

                // Add piece info
                formattedTransaction.put("piece", Map.of(
                        "id", piece.get().getId(),
                        "name", piece.get().getName(),
                        "reference", piece.get().getReference(),
                        "category", piece.get().getCategory()
                ));

                // Add student info if available
                if (transaction.getStudent() != null) {
                    try {
                        Student student = transaction.getStudent();
                        formattedTransaction.put("student", Map.of(
                                "id", student.getId(),
                                "fullName", student.getFullName(),
                                "studentCode", student.getStudentCode()
                        ));
                    } catch (Exception e) {
                        // If student can't be loaded, add basic info
                        formattedTransaction.put("student", Map.of(
                                "id", transaction.getStudent().getId(),
                                "fullName", "Non disponible",
                                "studentCode", "Non disponible"
                        ));
                    }
                }

                formattedTransactions.add(formattedTransaction);
            }

            return ResponseEntity.ok(formattedTransactions);

        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Erreur lors de la rÃ©cupÃ©ration des transactions: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    // POST - create new piece
    @PostMapping
    public ResponseEntity<?> createPiece(@RequestBody PieceRequest request) {
        try {
            // Validate required fields
            if (request.getName() == null || request.getName().trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("Le nom de la piÃ¨ce est requis"));
            }

            if (request.getReference() != null && pieceRepository.existsByReference(request.getReference())) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("Cette rÃ©fÃ©rence existe dÃ©jÃ "));
            }

            Piece piece = new Piece();
            piece.setName(request.getName());
            piece.setReference(request.getReference());
            piece.setCategory(request.getCategory());
            piece.setSubcategory(request.getSubcategory());
            piece.setInitialStock(request.getInitialStock());
            piece.setCurrentStock(request.getInitialStock());
            piece.setMinThreshold(request.getMinThreshold());
            piece.setMaxThreshold(request.getMaxThreshold());
            piece.setUnitPrice(request.getUnitPrice());
            piece.setPieceType(request.getPieceType());
            piece.setLocation(request.getLocation());
            piece.setShelfPosition(request.getShelfPosition());
            piece.setSupplier(request.getSupplier());
            piece.setSupplierReference(request.getSupplierReference());
            piece.setDescription(request.getDescription());
            piece.setNotes(request.getNotes());
            piece.setImageUrl(request.getImageUrl());

            // Set Excel-specific fields
            piece.setUnitType(request.getUnitType());
            piece.setQuantityPerUnit(request.getQuantityPerUnit());
            piece.setEntries(0);
            piece.setExits(0);

            Piece savedPiece = pieceRepository.save(piece);
            return ResponseEntity.status(HttpStatus.CREATED).body(savedPiece);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Erreur lors de la crÃ©ation: " + e.getMessage()));
        }
    }

    // PUT - update existing piece
    @PutMapping("/{id}")
    public ResponseEntity<?> updatePiece(@PathVariable Long id, @RequestBody PieceUpdateRequest request) {
        Optional<Piece> existingPiece = pieceRepository.findById(id);

        if (!existingPiece.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(createErrorResponse("PiÃ¨ce non trouvÃ©e"));
        }

        try {
            Piece piece = existingPiece.get();

            if (request.getName() != null) {
                piece.setName(request.getName());
            }

            if (request.getReference() != null &&
                    !request.getReference().equals(piece.getReference())) {
                if (pieceRepository.existsByReference(request.getReference())) {
                    return ResponseEntity.badRequest()
                            .body(createErrorResponse("Cette rÃ©fÃ©rence existe dÃ©jÃ "));
                }
                piece.setReference(request.getReference());
            }

            if (request.getCategory() != null) {
                piece.setCategory(request.getCategory());
            }

            if (request.getSubcategory() != null) {
                piece.setSubcategory(request.getSubcategory());
            }

            if (request.getMinThreshold() != null) {
                piece.setMinThreshold(request.getMinThreshold());
            }

            if (request.getMaxThreshold() != null) {
                piece.setMaxThreshold(request.getMaxThreshold());
            }

            if (request.getUnitPrice() != null) {
                piece.setUnitPrice(request.getUnitPrice());
            }

            if (request.getPieceType() != null) {
                piece.setPieceType(request.getPieceType());
            }

            if (request.getLocation() != null) {
                piece.setLocation(request.getLocation());
            }

            if (request.getShelfPosition() != null) {
                piece.setShelfPosition(request.getShelfPosition());
            }

            if (request.getSupplier() != null) {
                piece.setSupplier(request.getSupplier());
            }

            if (request.getSupplierReference() != null) {
                piece.setSupplierReference(request.getSupplierReference());
            }

            if (request.getDescription() != null) {
                piece.setDescription(request.getDescription());
            }

            if (request.getNotes() != null) {
                piece.setNotes(request.getNotes());
            }

            if (request.getImageUrl() != null) {
                piece.setImageUrl(request.getImageUrl());
            }

            // Update Excel-specific fields
            if (request.getUnitType() != null) {
                piece.setUnitType(request.getUnitType());
            }

            if (request.getQuantityPerUnit() != null) {
                piece.setQuantityPerUnit(request.getQuantityPerUnit());
            }

            Piece updatedPiece = pieceRepository.save(piece);
            return ResponseEntity.ok(updatedPiece);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Erreur lors de la mise Ã  jour: " + e.getMessage()));
        }
    }

    // PUT - adjust stock manually
    // Replace the existing adjustStock method in PieceController.java

    @PutMapping("/{id}/stock")
    public ResponseEntity<?> adjustStock(@PathVariable Long id, @RequestBody StockAdjustmentRequest request) {
        Optional<Piece> existingPiece = pieceRepository.findById(id);

        if (!existingPiece.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(createErrorResponse("Piece not found"));
        }

        try {
            Piece piece = existingPiece.get();

            // Initialize entries and exits if they are null
            if (piece.getEntries() == null) {
                piece.setEntries(0);
            }
            if (piece.getExits() == null) {
                piece.setExits(0);
            }

            int oldStock = piece.getCurrentStock();
            int adjustment;
            int newStock;

            if (request.getNewStock() != null) {
                adjustment = request.getNewStock() - oldStock;
                piece.setCurrentStock(request.getNewStock());
                newStock = request.getNewStock();
            } else if (request.getAdjustment() != null) {
                adjustment = request.getAdjustment();
                piece.setCurrentStock(piece.getCurrentStock() + adjustment);
                newStock = piece.getCurrentStock();
            } else {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("New stock value or adjustment amount is required"));
            }

            // Update entries/exits based on adjustment (with null checks)
            if (adjustment > 0) {
                piece.setEntries(piece.getEntries() + adjustment);
            } else if (adjustment < 0) {
                piece.setExits(piece.getExits() + Math.abs(adjustment));
            }

            // Ensure stock doesn't go negative
            if (piece.getCurrentStock() < 0) {
                piece.setCurrentStock(0);
                newStock = 0;
            }

            Piece updatedPiece = pieceRepository.save(piece);

            // Create a transaction to track this adjustment
            boolean createTransaction = request.getCreateTransaction() == null || request.getCreateTransaction();
            if (createTransaction) {
                try {
                    Transaction transaction = new Transaction();
                    transaction.setPiece(piece);
                    transaction.setQuantity(adjustment);
                    transaction.setTransactionType(Transaction.TransactionType.ADJUSTMENT);

                    // Store detailed information in the notes
                    String adjustmentNotes = "Manual adjustment: " +
                            (request.getReason() != null ? request.getReason() : "Stock modification") +
                            ", previous stock: " + oldStock +
                            ", new stock: " + newStock +
                            ", adjustment: " + (adjustment > 0 ? "+" + adjustment : adjustment);

                    transaction.setNotes(adjustmentNotes);

                    // Get or create a system student for the transaction
                    Student systemStudent = getOrCreateSystemStudent();
                    transaction.setStudent(systemStudent);
                    transaction.setAuthorizedBy("SYSTEM");

                    transactionRepository.save(transaction);
                } catch (Exception e) {
                    // Log the error but don't fail the stock adjustment
                    System.err.println("Warning: Could not create adjustment transaction: " + e.getMessage());
                }
            }

            Map<String, Object> response = new HashMap<>();
            response.put("piece", updatedPiece);
            response.put("oldStock", oldStock);
            response.put("newStock", updatedPiece.getCurrentStock());
            response.put("adjustment", adjustment);
            response.put("message", "Stock adjusted successfully");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace(); // For debugging
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Error adjusting stock: " + e.getMessage()));
        }
    }

    // Also update the getOrCreateSystemStudent method to handle potential issues
    private Student getOrCreateSystemStudent() {
        try {
            return studentRepository.findByStudentCode("SYSTEM")
                    .orElseGet(() -> {
                        Student sys = new Student("SYSTEM", "SYSTEM");
                        sys.setStatus(Student.StudentStatus.ACTIVE);
                        sys.setFullName("System User");
                        return studentRepository.save(sys);
                    });
        } catch (Exception e) {
            // If we can't create/find system student, create a minimal one
            Student sys = new Student();
            sys.setFullName("System User");
            sys.setStudentCode("SYSTEM");
            sys.setStatus(Student.StudentStatus.ACTIVE);
            try {
                return studentRepository.save(sys);
            } catch (Exception ex) {
                // Return existing system student or null if all fails
                return studentRepository.findByStudentCode("SYSTEM").orElse(null);
            }
        }
    }





    @GetMapping("/by-month")
    public ResponseEntity<?> getPiecesByMonth(
            @RequestParam(required = true) int month,
            @RequestParam(required = true) int year) {

        try {
            // Get pieces with activity in the specified month
            List<Piece> pieces = pieceRepository.findByActivityMonth(month, year);

            // If no pieces found for this month, return all pieces
            if (pieces.isEmpty()) {
                pieces = pieceRepository.findAllOrderedById();
            }

            return ResponseEntity.ok(pieces);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Erreur lors de la rÃ©cupÃ©ration des piÃ¨ces par mois: " + e.getMessage()));
        }
    }

    /**
     * Update piece QR code
     */
    @PutMapping("/{id}/qr-code")
    public ResponseEntity<?> updatePieceQrCode(@PathVariable Long id, @RequestBody QrCodeUpdateRequest request) {
        Optional<Piece> existingPiece = pieceRepository.findById(id);

        if (!existingPiece.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(createErrorResponse("PiÃ¨ce non trouvÃ©e"));
        }

        try {
            Piece piece = existingPiece.get();

            // Check if QR code is already used by another piece
            if (request.getQrCode() != null && !request.getQrCode().trim().isEmpty()) {
                Optional<Piece> existingQrPiece = pieceRepository.findByQrCode(request.getQrCode());
                if (existingQrPiece.isPresent() && !existingQrPiece.get().getId().equals(id)) {
                    return ResponseEntity.badRequest()
                            .body(createErrorResponse("Ce code QR est dÃ©jÃ  utilisÃ© par une autre piÃ¨ce"));
                }
            }

            piece.setQrCode(request.getQrCode());
            Piece updatedPiece = pieceRepository.save(piece);

            Map<String, Object> response = new HashMap<>();
            response.put("piece", updatedPiece);
            response.put("message", "Code QR mis Ã  jour avec succÃ¨s");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Erreur lors de la mise Ã  jour du code QR: " + e.getMessage()));
        }
    }

    /**
     * Get piece by QR code
     */
    @GetMapping("/qr-code/{qrCode}")
    public ResponseEntity<?> getPieceByQrCode(@PathVariable String qrCode) {
        try {
            Optional<Piece> piece = pieceRepository.findByQrCode(qrCode);
            if (piece.isPresent()) {
                return ResponseEntity.ok(piece.get());
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(createErrorResponse("Aucune piÃ¨ce trouvÃ©e avec ce code QR: " + qrCode));
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Erreur lors de la recherche: " + e.getMessage()));
        }
    }
    // DELETE piece
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deletePiece(@PathVariable Long id) {
        if (!pieceRepository.existsById(id)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(createErrorResponse("PiÃ¨ce non trouvÃ©e"));
        }

        // Check if piece has transactions
        Optional<Piece> piece = pieceRepository.findById(id);
        List<Transaction> transactions = transactionRepository.findByPiece(piece.get());

        if (!transactions.isEmpty()) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(createErrorResponse("Impossible de supprimer cette piÃ¨ce car elle a des transactions associÃ©es"));
        }

        try {
            pieceRepository.deleteById(id);
            return ResponseEntity.ok(createSuccessResponse("PiÃ¨ce supprimÃ©e avec succÃ¨s"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Erreur lors de la suppression: " + e.getMessage()));
        }
    }

    @PutMapping("/{id}/thresholds")
    public ResponseEntity<?> updateThresholds(@PathVariable Long id, @RequestBody ThresholdUpdateRequest request) {
        Optional<Piece> existingPiece = pieceRepository.findById(id);

        if (!existingPiece.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(createErrorResponse("Piece not found"));
        }

        try {
            Piece piece = existingPiece.get();

            piece.setMinThreshold(request.getMinThreshold());
            piece.setMaxThreshold(request.getMaxThreshold());

            Piece updatedPiece = pieceRepository.save(piece);
            return ResponseEntity.ok(updatedPiece);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Error updating thresholds: " + e.getMessage()));
        }
    }

    // Add this class
    public static class ThresholdUpdateRequest {
        private Integer minThreshold;
        private Integer maxThreshold;

        // Getters and setters
        public Integer getMinThreshold() { return minThreshold; }
        public void setMinThreshold(Integer minThreshold) { this.minThreshold = minThreshold; }

        public Integer getMaxThreshold() { return maxThreshold; }
        public void setMaxThreshold(Integer maxThreshold) { this.maxThreshold = maxThreshold; }
    }


    // GET statistics
    @GetMapping("/statistics")
    public ResponseEntity<?> getStatistics() {
        try {
            Map<String, Object> statistics = new HashMap<>();

            statistics.put("totalPieces", pieceRepository.count());
            statistics.put("lowStockCount", pieceRepository.findLowStockPieces().size());
            statistics.put("outOfStockCount", pieceRepository.findOutOfStockPieces().size());
            statistics.put("totalInventoryValue", pieceRepository.getTotalInventoryValue());
            statistics.put("categoryStatistics", pieceRepository.getCategoryStatistics());

            return ResponseEntity.ok(statistics);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Erreur lors du calcul des statistiques: " + e.getMessage()));
        }
    }

    @PostMapping("/{id}/image")
    public ResponseEntity<?> uploadImage(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
        System.out.println("=== IMAGE UPLOAD START ===");
        System.out.println("Piece ID: " + id);
        System.out.println("File name: " + file.getOriginalFilename());
        System.out.println("File size: " + file.getSize());

        Optional<Piece> pieceOpt = pieceRepository.findById(id);
        if (!pieceOpt.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(createErrorResponse("Piece not found"));
        }

        try {
            Piece piece = pieceOpt.get();

            // Validate file
            if (file.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("Please select a file to upload"));
            }

            // Check file type
            String contentType = file.getContentType();
            System.out.println("Content type: " + contentType);
            if (contentType == null || !contentType.startsWith("image/")) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("Please upload a valid image file"));
            }

            // Create uploads directory in project root (not in resources)
            String uploadDir = pieceUploadDir + "/pieces/";
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
                System.out.println("Created directory: " + uploadPath.toAbsolutePath());
            }

            // Generate unique filename
            String originalFilename = StringUtils.cleanPath(file.getOriginalFilename());
            String fileExtension = "";
            if (originalFilename.contains(".")) {
                fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }

            String filename = piece.getId() + "_" + System.currentTimeMillis() + fileExtension;

            // Save the file
            Path filePath = uploadPath.resolve(filename);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            System.out.println("File saved to: " + filePath.toAbsolutePath());

            // Update piece with new image URL (using our custom endpoint)
            String imageUrl = "/api/pieces/image/" + filename;
            piece.setImageUrl(imageUrl);
            pieceRepository.save(piece);

            System.out.println("Piece updated with imageUrl: " + imageUrl);
            System.out.println("=== IMAGE UPLOAD SUCCESS ===");

            // Return success response
            Map<String, Object> response = new HashMap<>();
            response.put("imageUrl", imageUrl);
            response.put("message", "Image uploaded successfully");
            response.put("filename", filename);

            return ResponseEntity.ok(response);

        } catch (IOException e) {
            System.err.println("Error uploading image: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Error uploading image: " + e.getMessage()));
        }
    }

    // 2. ADD this method to serve images from the uploads folder
    @GetMapping("/image/{filename}")
    public ResponseEntity<Resource> serveImage(@PathVariable String filename) {
        try {
            System.out.println("=== SERVING IMAGE ===");
            System.out.println("Requested filename: " + filename);

            Path imagePath = Paths.get(pieceUploadDir + "/pieces/" + filename);
            System.out.println("Looking for file at: " + imagePath.toAbsolutePath());

            Resource resource = new UrlResource(imagePath.toUri());

            if (resource.exists() && resource.isReadable()) {
                System.out.println("File found and readable");

                // Determine content type
                String contentType = Files.probeContentType(imagePath);
                if (contentType == null) {
                    contentType = "image/jpeg"; // default
                }
                System.out.println("Content type: " + contentType);

                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(contentType))
                        .header(HttpHeaders.CACHE_CONTROL, "max-age=3600")
                        .body(resource);
            } else {
                System.err.println("File not found or not readable");
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            System.err.println("Error serving image: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    @GetMapping("/{id}/image/{filename}")
    public ResponseEntity<Resource> getImage(@PathVariable Long id, @PathVariable String filename) {
        try {
            Path imagePath = Paths.get(pieceUploadDir + "/pieces/" + filename);
            Resource resource = new UrlResource(imagePath.toUri());

            if (resource.exists() && resource.isReadable()) {
                // Determine content type
                String contentType = Files.probeContentType(imagePath);
                if (contentType == null) {
                    contentType = "image/jpeg"; // default
                }

                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(contentType))
                        .header(HttpHeaders.CACHE_CONTROL, "max-age=3600") // Cache for 1 hour
                        .body(resource);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Add this method to your PieceController.java or create a separate service

    @PostMapping("/initialize-pieces")
    public ResponseEntity<?> initializePieces() {
        try {
            List<Piece> allPieces = pieceRepository.findAll();
            int updatedCount = 0;

            for (Piece piece : allPieces) {
                boolean needsUpdate = false;

                // Initialize entries if null
                if (piece.getEntries() == null) {
                    piece.setEntries(0);
                    needsUpdate = true;
                }

                // Initialize exits if null
                if (piece.getExits() == null) {
                    piece.setExits(0);
                    needsUpdate = true;
                }

                // Initialize current stock if null
                if (piece.getCurrentStock() == null) {
                    piece.setCurrentStock(piece.getInitialStock() != null ? piece.getInitialStock() : 0);
                    needsUpdate = true;
                }

                if (needsUpdate) {
                    pieceRepository.save(piece);
                    updatedCount++;
                }
            }

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Pieces initialized successfully");
            response.put("totalPieces", allPieces.size());
            response.put("updatedPieces", updatedCount);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Error initializing pieces: " + e.getMessage()));
        }
    }

    // Alternative: Add this to your application startup
    @PostConstruct
    public void initializePiecesOnStartup() {
        try {
            List<Piece> piecesWithNullValues = pieceRepository.findAll()
                    .stream()
                    .filter(piece -> piece.getEntries() == null || piece.getExits() == null)
                    .collect(Collectors.toList());

            if (!piecesWithNullValues.isEmpty()) {
                System.out.println("Initializing " + piecesWithNullValues.size() + " pieces with null values...");

                for (Piece piece : piecesWithNullValues) {
                    if (piece.getEntries() == null) piece.setEntries(0);
                    if (piece.getExits() == null) piece.setExits(0);
                    pieceRepository.save(piece);
                }

                System.out.println("Piece initialization completed.");
            }
        } catch (Exception e) {
            System.err.println("Error during piece initialization: " + e.getMessage());
        }
    }

// Don't forget to add these imports:


    // GET all categories
    @GetMapping("/categories")
    public List<String> getAllCategories() {
        return pieceRepository.findAllCategories();
    }

    // GET all locations
    @GetMapping("/locations")
    public List<String> getAllLocations() {
        return pieceRepository.findAllLocations();
    }

    // GET all suppliers
    @GetMapping("/suppliers")
    public List<String> getAllSuppliers() {
        return pieceRepository.findAllSuppliers();
    }

    // GET all unit types
    @GetMapping("/unit-types")
    public List<String> getAllUnitTypes() {
        return pieceRepository.findAllUnitTypes();
    }

    // Excel-specific endpoints

    // GET piece with Excel format
    @GetMapping("/excel-format")
    public ResponseEntity<?> getPiecesInExcelFormat() {
        try {
            List<Piece> pieces = pieceRepository.findAllOrderedById();

            // Transform to Excel format if needed
            return ResponseEntity.ok(pieces);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Erreur lors de l'export: " + e.getMessage()));
        }
    }

    // New endpoint to get adjustment history

/**
 * Additional endpoints for PieceController.java
 * These need to be added to the existing PieceController class
 */

    /**
     * Get adjustment history - this endpoint retrieves all stock adjustments
     * for generating reports
     */
    @GetMapping("/adjustment-history")
    public ResponseEntity<?> getAdjustmentHistory() {
        try {
            // We'll use transactions of type ADJUSTMENT to track stock adjustments
            List<Transaction> adjustments = transactionRepository.findByTransactionTypeOrderByTransactionDateDesc(
                    Transaction.TransactionType.ADJUSTMENT);

            // Transform transactions to a more convenient format for the frontend
            List<Map<String, Object>> formattedAdjustments = new ArrayList<>();

            for (Transaction transaction : adjustments) {
                Map<String, Object> adjustment = new HashMap<>();
                adjustment.put("id", transaction.getId());
                adjustment.put("date", transaction.getTransactionDate());

                // Get piece information
                if (transaction.getPiece() != null) {
                    adjustment.put("pieceId", transaction.getPiece().getId());
                    adjustment.put("pieceName", transaction.getPiece().getName());
                    adjustment.put("pieceReference", transaction.getPiece().getReference());
                    adjustment.put("pieceCategory", transaction.getPiece().getCategory());
                } else {
                    adjustment.put("pieceId", null);
                    adjustment.put("pieceName", "Unknown");
                    adjustment.put("pieceReference", "N/A");
                    adjustment.put("pieceCategory", "N/A");
                }

                adjustment.put("adjustment", transaction.getQuantity());
                adjustment.put("authorizedBy", "SYSTEM"); // Use a default value

                // Parse the notes to extract old and new stock values if available
                String notes = transaction.getNotes();
                if (notes != null) {
                    adjustment.put("reason", notes);

                    // Try to extract old and new stock values
                    try {
                        if (notes.contains("ancien:") && notes.contains("nouveau:")) {
                            int oldStockIndex = notes.indexOf("ancien:") + 7;
                            int commaIndex = notes.indexOf(",", oldStockIndex);
                            int newStockIndex = notes.indexOf("nouveau:") + 8;

                            String oldStockStr = notes.substring(oldStockIndex, commaIndex).trim();
                            String newStockStr = notes.substring(newStockIndex).trim();

                            adjustment.put("oldStock", Integer.parseInt(oldStockStr));
                            adjustment.put("newStock", Integer.parseInt(newStockStr));
                        } else {
                            adjustment.put("oldStock", 0);
                            adjustment.put("newStock", 0);
                        }
                    } catch (Exception e) {
                        // If parsing fails, set default values
                        adjustment.put("oldStock", 0);
                        adjustment.put("newStock", 0);
                    }
                } else {
                    adjustment.put("reason", "");
                    adjustment.put("oldStock", 0);
                    adjustment.put("newStock", 0);
                }

                formattedAdjustments.add(adjustment);
            }

            return ResponseEntity.ok(formattedAdjustments);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Erreur lors de la rÃ©cupÃ©ration de l'historique des ajustements: " + e.getMessage()));
        }
    }


    // Helper methods
    private Map<String, String> createErrorResponse(String message) {
        Map<String, String> response = new HashMap<>();
        response.put("error", message);
        return response;
    }

    private Map<String, String> createSuccessResponse(String message) {
        Map<String, String> response = new HashMap<>();
        response.put("message", message);
        return response;
    }

    // Request classes
    public static class PieceRequest {
        private String name;
        private String reference;
        private String category;
        private String subcategory;
        private Integer initialStock;
        private Integer minThreshold;
        private Integer maxThreshold;
        private Double unitPrice;
        private Piece.PieceType pieceType;
        private String location;
        private String shelfPosition;
        private String supplier;
        private String supplierReference;
        private String description;
        private String notes;
        private String imageUrl;
        private String unitType;
        private Integer quantityPerUnit;

        // Getters and setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getReference() { return reference; }
        public void setReference(String reference) { this.reference = reference; }

        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }

        public String getSubcategory() { return subcategory; }
        public void setSubcategory(String subcategory) { this.subcategory = subcategory; }

        public Integer getInitialStock() { return initialStock; }
        public void setInitialStock(Integer initialStock) { this.initialStock = initialStock; }

        public Integer getMinThreshold() { return minThreshold; }
        public void setMinThreshold(Integer minThreshold) { this.minThreshold = minThreshold; }

        public Integer getMaxThreshold() { return maxThreshold; }
        public void setMaxThreshold(Integer maxThreshold) { this.maxThreshold = maxThreshold; }

        public Double getUnitPrice() { return unitPrice; }
        public void setUnitPrice(Double unitPrice) { this.unitPrice = unitPrice; }

        public Piece.PieceType getPieceType() { return pieceType; }
        public void setPieceType(Piece.PieceType pieceType) { this.pieceType = pieceType; }

        public String getLocation() { return location; }
        public void setLocation(String location) { this.location = location; }

        public String getShelfPosition() { return shelfPosition; }
        public void setShelfPosition(String shelfPosition) { this.shelfPosition = shelfPosition; }

        public String getSupplier() { return supplier; }
        public void setSupplier(String supplier) { this.supplier = supplier; }

        public String getSupplierReference() { return supplierReference; }
        public void setSupplierReference(String supplierReference) { this.supplierReference = supplierReference; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        public String getNotes() { return notes; }
        public void setNotes(String notes) { this.notes = notes; }

        public String getImageUrl() { return imageUrl; }
        public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

        public String getUnitType() { return unitType; }
        public void setUnitType(String unitType) { this.unitType = unitType; }

        public Integer getQuantityPerUnit() { return quantityPerUnit; }
        public void setQuantityPerUnit(Integer quantityPerUnit) { this.quantityPerUnit = quantityPerUnit; }
    }

    public static class PieceUpdateRequest {
        private String name;
        private String reference;
        private String category;
        private String subcategory;
        private Integer minThreshold;
        private Integer maxThreshold;
        private Double unitPrice;
        private Piece.PieceType pieceType;
        private String location;
        private String shelfPosition;
        private String supplier;
        private String supplierReference;
        private String description;
        private String notes;
        private String imageUrl;
        private String unitType;
        private Integer quantityPerUnit;

        // Getters and setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getReference() { return reference; }
        public void setReference(String reference) { this.reference = reference; }

        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }

        public String getSubcategory() { return subcategory; }
        public void setSubcategory(String subcategory) { this.subcategory = subcategory; }

        public Integer getMinThreshold() { return minThreshold; }
        public void setMinThreshold(Integer minThreshold) { this.minThreshold = minThreshold; }

        public Integer getMaxThreshold() { return maxThreshold; }
        public void setMaxThreshold(Integer maxThreshold) { this.maxThreshold = maxThreshold; }

        public Double getUnitPrice() { return unitPrice; }
        public void setUnitPrice(Double unitPrice) { this.unitPrice = unitPrice; }

        public Piece.PieceType getPieceType() { return pieceType; }
        public void setPieceType(Piece.PieceType pieceType) { this.pieceType = pieceType; }

        public String getLocation() { return location; }
        public void setLocation(String location) { this.location = location; }

        public String getShelfPosition() { return shelfPosition; }
        public void setShelfPosition(String shelfPosition) { this.shelfPosition = shelfPosition; }

        public String getSupplier() { return supplier; }
        public void setSupplier(String supplier) { this.supplier = supplier; }

        public String getSupplierReference() { return supplierReference; }
        public void setSupplierReference(String supplierReference) { this.supplierReference = supplierReference; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        public String getNotes() { return notes; }
        public void setNotes(String notes) { this.notes = notes; }

        public String getImageUrl() { return imageUrl; }
        public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

        public String getUnitType() { return unitType; }
        public void setUnitType(String unitType) { this.unitType = unitType; }

        public Integer getQuantityPerUnit() { return quantityPerUnit; }
        public void setQuantityPerUnit(Integer quantityPerUnit) { this.quantityPerUnit = quantityPerUnit; }
    }

    public static class StockAdjustmentRequest {
        private Integer newStock;
        private Integer adjustment;
        private Boolean createTransaction;
        private String reason;

        // Getters and setters
        public Integer getNewStock() { return newStock; }
        public void setNewStock(Integer newStock) { this.newStock = newStock; }

        public Integer getAdjustment() { return adjustment; }
        public void setAdjustment(Integer adjustment) { this.adjustment = adjustment; }

        public Boolean getCreateTransaction() { return createTransaction; }
        public void setCreateTransaction(Boolean createTransaction) { this.createTransaction = createTransaction; }

        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
    }
    public static class QrCodeUpdateRequest {
        private String qrCode;

        public String getQrCode() {
            return qrCode;
        }

        public void setQrCode(String qrCode) {
            this.qrCode = qrCode;
        }
    }
}