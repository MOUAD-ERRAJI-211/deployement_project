package orthoproconnect.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import orthoproconnect.model.*;
import orthoproconnect.repository.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private PieceRepository pieceRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private TeacherRepository teacherRepository;

    // GET all transactions
    @GetMapping
    public List<Transaction> getAllTransactions() {
        return transactionRepository.findAll();
    }

    // GET transaction by ID
    @GetMapping("/{id}")
    public ResponseEntity<?> getTransactionById(@PathVariable Long id) {
        Optional<Transaction> transaction = transactionRepository.findById(id);
        if (transaction.isPresent()) {
            return ResponseEntity.ok(transaction.get());
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(createErrorResponse("Transaction non trouvÃ©e"));
        }
    }

    // POST - checkout items
    @PostMapping("/checkout")
    public ResponseEntity<?> checkoutItems(@RequestBody CheckoutRequest request) {
        try {
            // Validate student
            Optional<Student> student = studentRepository.findByStudentCode(request.getStudentCode());
            if (!student.isPresent()) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("Ã‰tudiant non trouvÃ©"));
            }

            List<Transaction> createdTransactions = new ArrayList<>();
            List<String> errors = new ArrayList<>();

            // Process each item
            for (CheckoutItem item : request.getItems()) {
                Optional<Piece> piece = pieceRepository.findById(item.getPieceId());
                if (!piece.isPresent()) {
                    errors.add("PiÃ¨ce non trouvÃ©e avec ID: " + item.getPieceId());
                    continue;
                }

                // Validate checkout constraints
                if (!validateCheckoutConstraints(piece.get(), student.get(), item.getQuantity())) {
                    errors.add("Contraintes de sortie non respectÃ©es pour: " + piece.get().getName());
                    continue;
                }

                // Create transaction
                Transaction transaction = new Transaction(
                        piece.get(),
                        student.get(),
                        -item.getQuantity(), // Negative for checkout
                        Transaction.TransactionType.CHECKOUT,
                        request.getAuthorizedBy()
                );

                if (item.getExpectedReturnDate() != null) {
                    transaction.setExpectedReturnDate(LocalDateTime.parse(item.getExpectedReturnDate()));
                }

                if (request.getAdditionalStudents() != null && !request.getAdditionalStudents().isEmpty()) {
                    transaction.setAdditionalStudents(
                            new ObjectMapper().writeValueAsString(request.getAdditionalStudents()));
                }

                if (request.getNotes() != null) {
                    transaction.setNotes(request.getNotes());
                }

                // Update piece stock
                piece.get().updateStock(-item.getQuantity());
                pieceRepository.save(piece.get());

                // Save transaction
                transaction = transactionRepository.save(transaction);
                createdTransactions.add(transaction);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("transactions", createdTransactions);
            response.put("successful", createdTransactions.size());
            response.put("errors", errors);
            response.put("message", createdTransactions.size() + " sortie(s) effectuÃ©e(s) avec succÃ¨s");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Erreur lors de la sortie: " + e.getMessage()));
        }
    }

    // POST - return items
    @PostMapping("/return")
    public ResponseEntity<?> returnItems(@RequestBody ReturnRequest request) {
        try {
            Optional<Student> student = studentRepository.findByStudentCode(request.getStudentCode());
            if (!student.isPresent()) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("Ã‰tudiant non trouvÃ©"));
            }

            List<Optional<Transaction>> returnedTransactions = new ArrayList<>();
            List<String> errors = new ArrayList<>();

            for (Long transactionId : request.getTransactionIds()) {
                Optional<Transaction> transaction = transactionRepository.findById(transactionId);
                if (!transaction.isPresent()) {
                    errors.add("Transaction non trouvÃ©e avec ID: " + transactionId);
                    continue;
                }

                if (!transaction.get().canBeReturned()) {
                    errors.add("Transaction non retournable avec ID: " + transactionId);
                    continue;
                }

                // Mark as returned
                transaction.get().markAsReturned();
                if (request.getNotes() != null) {
                    transaction.get().setNotes(request.getNotes());
                }
                if (request.getAuthorizedBy() != null) {
                    transaction.get().setAuthorizedBy(request.getAuthorizedBy());
                }

                // Update piece stock
                Piece piece = transaction.get().getPiece();
                piece.updateStock(Math.abs(transaction.get().getQuantity()));
                pieceRepository.save(piece);

                // Save transaction
                transaction = Optional.of(transactionRepository.save(transaction.get()));
                returnedTransactions.add(transaction);
            }

            // Update student activity
            student.get().updateActivity();
            studentRepository.save(student.get());

            Map<String, Object> response = new HashMap<>();
            response.put("transactions", returnedTransactions);
            response.put("successful", returnedTransactions.size());
            response.put("errors", errors);
            response.put("message", returnedTransactions.size() + " retour(s) effectuÃ©(s) avec succÃ¨s");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Erreur lors du retour: " + e.getMessage()));
        }
    }

    @PostMapping("/search")
    public ResponseEntity<?> searchTransactions(@RequestBody TransactionSearchRequest request) {
        try {
            List<Transaction> transactions = transactionRepository.findByMultipleCriteria(
                    request.getPieceId(),
                    request.getStudentId(),
                    request.getAuthorizedBy(),
                    request.getTransactionType(),
                    request.getStatus(),
                    request.getStartDate(),
                    request.getEndDate()
            );

            // Ensure transactions are initialized
            List<Map<String, Object>> formattedTransactions = new ArrayList<>();
            for (Transaction t : transactions) {
                Map<String, Object> transaction = new HashMap<>();
                transaction.put("id", t.getId());
                transaction.put("quantity", t.getQuantity());
                transaction.put("transactionType", t.getTransactionType());
                transaction.put("transactionDate", t.getTransactionDate());
                transaction.put("expectedReturnDate", t.getExpectedReturnDate());
                transaction.put("actualReturnDate", t.getActualReturnDate());
                transaction.put("status", t.getStatus());
                transaction.put("returnedQuantity", t.getReturnedQuantity());

                // Include piece information
                if (t.getPiece() != null) {
                    Map<String, Object> piece = new HashMap<>();
                    piece.put("id", t.getPiece().getId());
                    piece.put("name", t.getPiece().getName());
                    piece.put("category", t.getPiece().getCategory());
                    transaction.put("piece", piece);
                }

                // Include student information
                if (t.getStudent() != null) {
                    Map<String, Object> student = new HashMap<>();
                    student.put("id", t.getStudent().getId());
                    student.put("fullName", t.getStudent().getFullName());
                    student.put("studentCode", t.getStudent().getStudentCode());
                    transaction.put("student", student);
                }

                formattedTransactions.add(transaction);
            }

            return ResponseEntity.ok(formattedTransactions);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Error searching transactions: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    // POST - partial return (for multi-quantity items)
    @PostMapping("/partial-return")
    public ResponseEntity<?> partialReturn(@RequestBody PartialReturnRequest request) {
        try {
            Optional<Transaction> optTransaction = transactionRepository.findById(request.getTransactionId());

            if (!optTransaction.isPresent()) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("Transaction non trouvÃ©e"));
            }

            Transaction transaction = optTransaction.get();

            // Validate the transaction
            if (!transaction.canBeReturned()) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("Cette transaction ne peut pas Ãªtre retournÃ©e"));
            }

            if (request.getReturnedQuantity() <= 0 ||
                    request.getReturnedQuantity() > transaction.getRemainingQuantity()) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("QuantitÃ© de retour invalide"));
            }

            // Update transaction
            transaction.markAsPartiallyReturned(
                    transaction.getReturnedQuantity() + request.getReturnedQuantity());

            if (request.getNotes() != null) {
                transaction.setNotes(request.getNotes());
            }

            // Update piece stock
            Piece piece = transaction.getPiece();
            piece.updateStock(request.getReturnedQuantity());
            pieceRepository.save(piece);

            // Save transaction
            transaction = transactionRepository.save(transaction);

            Map<String, Object> response = new HashMap<>();
            response.put("transaction", transaction);
            response.put("message", "Retour partiel effectuÃ© avec succÃ¨s");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Erreur lors du retour partiel: " + e.getMessage()));
        }
    }

    // POST - manual stock adjustment transaction
    @PostMapping("/adjustment")
    public ResponseEntity<?> createAdjustmentTransaction(@RequestBody AdjustmentRequest request) {
        try {
            // Validate request
            if (request.getPieceId() == null) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("ID de la piÃ¨ce requis"));
            }

            Optional<Piece> piece = pieceRepository.findById(request.getPieceId());
            if (!piece.isPresent()) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("PiÃ¨ce non trouvÃ©e"));
            }

            // Create a system student for adjustments if not exists
            Student systemStudent = studentRepository.findByStudentCode("SYSTEM")
                    .orElseGet(() -> {
                        Student sys = new Student("SYSTEM", "SYSTEM");
                        sys.setStatus(Student.StudentStatus.ACTIVE);
                        return studentRepository.save(sys);
                    });

            Piece p = piece.get();
            int oldStock = p.getCurrentStock();

            // Create adjustment transaction
            Transaction transaction = new Transaction(p, systemStudent,
                    Math.abs(request.getAdjustment()),
                    Transaction.TransactionType.ADJUSTMENT,
                    request.getAuthorizedBy() != null ?
                            request.getAuthorizedBy() : "ADMIN");

            if (request.getReason() != null) {
                transaction.setNotes("Ajustement: " + request.getReason());
            }

            // Update piece stock
            p.setCurrentStock(p.getCurrentStock() + request.getAdjustment());
            if (p.getCurrentStock() < 0) {
                p.setCurrentStock(0);
            }
            pieceRepository.save(p);

            // Save transaction
            transaction = transactionRepository.save(transaction);

            Map<String, Object> response = new HashMap<>();
            response.put("transaction", transaction);
            response.put("oldStock", oldStock);
            response.put("newStock", p.getCurrentStock());
            response.put("adjustment", request.getAdjustment());
            response.put("message", "Ajustement de stock effectuÃ© avec succÃ¨s");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Erreur lors de l'ajustement: " + e.getMessage()));
        }
    }

    // GET transaction statistics
    @GetMapping("/statistics")
    public ResponseEntity<?> getTransactionStatistics(@RequestParam(defaultValue = "30") int days) {
        try {
            LocalDateTime startDate = LocalDateTime.now().minusDays(days);
            LocalDateTime endDate = LocalDateTime.now();

            Map<String, Object> statistics = new HashMap<>();

            // Basic counts
            statistics.put("totalTransactions", transactionRepository.count());
            statistics.put("pendingCheckouts", transactionRepository.findPendingCheckouts().size());
            statistics.put("overdueTransactions", transactionRepository.findOverdueTransactions(LocalDateTime.now()).size());

            // Statistics for the specified period
            statistics.put("recentTransactions", transactionRepository.findByDateRange(startDate, endDate).size());
            statistics.put("dailyStats", transactionRepository.getDailyTransactionStatistics(startDate, endDate));
            statistics.put("typeDistribution", transactionRepository.getTransactionTypeDistribution(startDate, endDate));
            statistics.put("mostActiveItems", transactionRepository.getMostActiveItems(startDate, endDate));
            statistics.put("mostActiveStudents", transactionRepository.getMostActiveStudents(startDate, endDate));
            statistics.put("hourlyDistribution", transactionRepository.getHourlyTransactionDistribution(startDate, endDate));
            statistics.put("categoryUsage", transactionRepository.getCategoryUsageStatistics(startDate, endDate));

            return ResponseEntity.ok(statistics);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Erreur lors du calcul des statistiques: " + e.getMessage()));
        }
    }

    // DELETE transaction (admin only)
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteTransaction(@PathVariable Long id) {
        Optional<Transaction> transaction = transactionRepository.findById(id);

        if (!transaction.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(createErrorResponse("Transaction non trouvÃ©e"));
        }

        try {
            // If it's a pending checkout, restore the stock
            Transaction t = transaction.get();
            if (t.getStatus() == Transaction.TransactionStatus.PENDING &&
                    t.getTransactionType() == Transaction.TransactionType.CHECKOUT) {

                Piece piece = t.getPiece();
                piece.updateStock(t.getQuantity());
                pieceRepository.save(piece);
            }

            transactionRepository.deleteById(id);
            return ResponseEntity.ok(createSuccessResponse("Transaction supprimÃ©e avec succÃ¨s"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Erreur lors de la suppression: " + e.getMessage()));
        }
    }

    // GET daily transaction summary
    @GetMapping("/daily-summary")
    public ResponseEntity<?> getDailyTransactionSummary(@RequestParam(required = false) String date) {
        try {
            LocalDateTime targetDate;
            if (date != null && !date.isEmpty()) {
                targetDate = LocalDateTime.parse(date + "T00:00:00");
            } else {
                targetDate = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);
            }

            LocalDateTime startOfDay = targetDate;
            LocalDateTime endOfDay = targetDate.plusDays(1).minusNanos(1);

            List<Transaction> dailyTransactions = transactionRepository.findByDateRange(startOfDay, endOfDay);

            Map<String, Object> summary = new HashMap<>();
            summary.put("date", startOfDay.toLocalDate());
            summary.put("totalTransactions", dailyTransactions.size());

            // Count by type
            Map<String, Long> typeCount = new HashMap<>();
            for (Transaction.TransactionType type : Transaction.TransactionType.values()) {
                long count = dailyTransactions.stream()
                        .filter(t -> t.getTransactionType() == type)
                        .count();
                typeCount.put(type.name(), count);
            }
            summary.put("transactionsByType", typeCount);

            // Count by hour
            Map<Integer, Long> hourlyCount = new HashMap<>();
            for (int hour = 0; hour < 24; hour++) {
                final int h = hour;
                long count = dailyTransactions.stream()
                        .filter(t -> t.getTransactionDate().getHour() == h)
                        .count();
                hourlyCount.put(hour, count);
            }
            summary.put("transactionsByHour", hourlyCount);

            // Most active pieces
            Map<String, Object> mostActivePieces = dailyTransactions.stream()
                    .collect(Collectors.groupingBy(
                            t -> t.getPiece().getName(),
                            Collectors.counting()))
                    .entrySet().stream()
                    .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                    .limit(5)
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            Map.Entry::getValue,
                            (e1, e2) -> e1,
                            LinkedHashMap::new));
            summary.put("mostActivePieces", mostActivePieces);

            return ResponseEntity.ok(summary);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Erreur lors du calcul du rÃ©sumÃ© quotidien: " + e.getMessage()));
        }
    }

    // GET student checkout history
    @GetMapping("/student/{studentCode}/history")
    public ResponseEntity<?> getStudentCheckoutHistory(
            @PathVariable String studentCode,
            @RequestParam(defaultValue = "30") int days) {
        try {
            // Find student by code or QR code
            Optional<Student> student = studentRepository.findByStudentCode(studentCode);
            if (!student.isPresent()) {
                student = studentRepository.findByQrCode(studentCode);
            }

            if (!student.isPresent()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(createErrorResponse("Ã‰tudiant non trouvÃ© avec le code: " + studentCode));
            }

            LocalDateTime since = LocalDateTime.now().minusDays(days);
            List<Transaction> history = transactionRepository.findByStudentAndDateRange(
                    student.get(), since, LocalDateTime.now());

            Map<String, Object> response = new HashMap<>();
            response.put("student", Map.of(
                    "id", student.get().getId(),
                    "fullName", student.get().getFullName(),
                    "studentCode", student.get().getStudentCode()
            ));
            response.put("history", history);
            response.put("totalTransactions", history.size());
            response.put("period", days + " derniers jours");

            // Statistics
            long checkouts = history.stream().filter(t -> t.getTransactionType() == Transaction.TransactionType.CHECKOUT).count();
            long returns = history.stream().filter(t -> t.getTransactionType() == Transaction.TransactionType.RETURN).count();
            long pending = history.stream().filter(t -> t.getStatus() == Transaction.TransactionStatus.PENDING).count();

            response.put("statistics", Map.of(
                    "checkouts", checkouts,
                    "returns", returns,
                    "pending", pending
            ));

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Erreur lors de la rÃ©cupÃ©ration de l'historique: " + e.getMessage()));
        }
    }

    // Method to validate checkout constraints
    private boolean validateCheckoutConstraints(Piece piece, Student student, int quantity) {
        // Check stock availability
        if (!piece.canCheckout(quantity)) {
            return false;
        }

        // For non-consumables, check if student already has one
        if (piece.getPieceType() != Piece.PieceType.CONSUMABLE) {
            List<Transaction> existingCheckouts = transactionRepository.findPendingCheckoutByPieceAndStudent(piece, student);
            if (!existingCheckouts.isEmpty()) {
                return false;
            }
        }

        return true;
    }

    // Method to calculate and update overdue status
    @GetMapping("/update-overdue")
    public ResponseEntity<?> updateOverdueTransactions() {
        try {
            List<Transaction> pendingTransactions = transactionRepository.findPendingCheckouts();
            LocalDateTime now = LocalDateTime.now();
            int updatedCount = 0;

            for (Transaction transaction : pendingTransactions) {
                if (transaction.getExpectedReturnDate() != null &&
                        transaction.getExpectedReturnDate().isBefore(now) &&
                        transaction.getStatus() == Transaction.TransactionStatus.PENDING) {

                    transaction.setStatus(Transaction.TransactionStatus.OVERDUE);
                    transactionRepository.save(transaction);
                    updatedCount++;
                }
            }

            Map<String, Object> response = new HashMap<>();
            response.put("message", updatedCount + " transaction(s) marquÃ©e(s) comme en retard");
            response.put("updatedCount", updatedCount);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Erreur lors de la mise Ã  jour des retards: " + e.getMessage()));
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
    public static class CheckoutRequest {
        private String studentCode;
        private List<CheckoutItem> items;
        private List<String> additionalStudents;
        private String authorizedBy;
        private String notes;

        // Getters and setters
        public String getStudentCode() { return studentCode; }
        public void setStudentCode(String studentCode) { this.studentCode = studentCode; }

        public List<CheckoutItem> getItems() { return items; }
        public void setItems(List<CheckoutItem> items) { this.items = items; }

        public List<String> getAdditionalStudents() { return additionalStudents; }
        public void setAdditionalStudents(List<String> additionalStudents) { this.additionalStudents = additionalStudents; }

        public String getAuthorizedBy() { return authorizedBy; }
        public void setAuthorizedBy(String authorizedBy) { this.authorizedBy = authorizedBy; }

        public String getNotes() { return notes; }
        public void setNotes(String notes) { this.notes = notes; }
    }

    public static class CheckoutItem {
        private Long pieceId;
        private Integer quantity;
        private String expectedReturnDate;

        // Getters and setters
        public Long getPieceId() { return pieceId; }
        public void setPieceId(Long pieceId) { this.pieceId = pieceId; }

        public Integer getQuantity() { return quantity; }
        public void setQuantity(Integer quantity) { this.quantity = quantity; }

        public String getExpectedReturnDate() { return expectedReturnDate; }
        public void setExpectedReturnDate(String expectedReturnDate) { this.expectedReturnDate = expectedReturnDate; }
    }

    @GetMapping("/test-return/{studentId}/{transactionId}")
    public ResponseEntity<?> testReturn(@PathVariable Long studentId, @PathVariable Long transactionId) {
        try {
            // Find the student
            Optional<Student> student = studentRepository.findById(studentId);
            if (!student.isPresent()) {
                return ResponseEntity.badRequest().body(createErrorResponse("Student not found"));
            }

            // Find the transaction
            Optional<Transaction> transaction = transactionRepository.findById(transactionId);
            if (!transaction.isPresent()) {
                return ResponseEntity.badRequest().body(createErrorResponse("Transaction not found"));
            }

            // Create a test return request
            ReturnRequest request = new ReturnRequest();
            request.setStudentCode(student.get().getStudentCode());
            request.setTransactionIds(List.of(transactionId));
            request.setNotes("Test return");
            request.setAuthorizedBy("TEST_API");

            // Process the request
            Map<String, Object> result = new HashMap<>();
            result.put("request", request);

            // Check if transaction can be returned
            boolean canBeReturned = transaction.get().canBeReturned();
            result.put("canBeReturned", canBeReturned);

            // Add transaction details
            Map<String, Object> transactionDetails = new HashMap<>();
            transactionDetails.put("id", transaction.get().getId());
            transactionDetails.put("transactionType", transaction.get().getTransactionType());
            transactionDetails.put("status", transaction.get().getStatus());
            transactionDetails.put("quantity", transaction.get().getQuantity());
            result.put("transaction", transactionDetails);

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Error in test return: " + e.getMessage()));
        }
    }
    public static class ReturnRequest {
        private String studentCode;
        private List<Long> transactionIds;
        private String notes;
        private String authorizedBy;

        // Getters and setters
        public String getStudentCode() { return studentCode; }
        public void setStudentCode(String studentCode) { this.studentCode = studentCode; }

        public List<Long> getTransactionIds() { return transactionIds; }
        public void setTransactionIds(List<Long> transactionIds) { this.transactionIds = transactionIds; }

        public String getNotes() { return notes; }
        public void setNotes(String notes) { this.notes = notes; }

        public String getAuthorizedBy() { return authorizedBy; }
        public void setAuthorizedBy(String authorizedBy) { this.authorizedBy = authorizedBy; }
    }

    public static class PartialReturnRequest {
        private Long transactionId;
        private Integer returnedQuantity;
        private String notes;

        // Getters and setters
        public Long getTransactionId() { return transactionId; }
        public void setTransactionId(Long transactionId) { this.transactionId = transactionId; }

        public Integer getReturnedQuantity() { return returnedQuantity; }
        public void setReturnedQuantity(Integer returnedQuantity) { this.returnedQuantity = returnedQuantity; }

        public String getNotes() { return notes; }
        public void setNotes(String notes) { this.notes = notes; }
    }

    public static class AdjustmentRequest {
        private Long pieceId;
        private Integer adjustment;
        private String reason;
        private String authorizedBy;

        // Getters and setters
        public Long getPieceId() { return pieceId; }
        public void setPieceId(Long pieceId) { this.pieceId = pieceId; }

        public Integer getAdjustment() { return adjustment; }
        public void setAdjustment(Integer adjustment) { this.adjustment = adjustment; }

        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }

        public String getAuthorizedBy() { return authorizedBy; }
        public void setAuthorizedBy(String authorizedBy) { this.authorizedBy = authorizedBy; }
    }


    public static class TransactionSearchRequest {
        private Long pieceId;
        private Long studentId;
        private String authorizedBy;
        private Transaction.TransactionType transactionType;
        private Transaction.TransactionStatus status;
        private LocalDateTime startDate;
        private LocalDateTime endDate;

        // Getters and setters
        public Long getPieceId() { return pieceId; }
        public void setPieceId(Long pieceId) { this.pieceId = pieceId; }

        public Long getStudentId() { return studentId; }
        public void setStudentId(Long studentId) { this.studentId = studentId; }

        public String getAuthorizedBy() { return authorizedBy; }
        public void setAuthorizedBy(String authorizedBy) { this.authorizedBy = authorizedBy; }

        public Transaction.TransactionType getTransactionType() { return transactionType; }
        public void setTransactionType(Transaction.TransactionType transactionType) { this.transactionType = transactionType; }

        public Transaction.TransactionStatus getStatus() { return status; }
        public void setStatus(Transaction.TransactionStatus status) { this.status = status; }

        public LocalDateTime getStartDate() { return startDate; }
        public void setStartDate(LocalDateTime startDate) { this.startDate = startDate; }

        public LocalDateTime getEndDate() { return endDate; }
        public void setEndDate(LocalDateTime endDate) { this.endDate = endDate; }
    }
}