package orthoproconnect.controller;

import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import orthoproconnect.model.Student;
import orthoproconnect.model.Transaction;
import orthoproconnect.repository.StudentRepository;
import orthoproconnect.repository.TransactionRepository;
import orthoproconnect.service.QrCodeService;

import java.time.LocalDateTime;
import java.util.*;

import org.springframework.http.MediaType;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

@RestController
@RequestMapping("/api/students")
public class StudentController {

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private QrCodeService qrCodeService;

    // GET all students
    @GetMapping
    public List<Student> getAllStudents() {
        return studentRepository.findAll();
    }

    // GET active students
    @GetMapping("/active")
    public List<Student> getActiveStudents() {
        return studentRepository.findActiveStudents();
    }

    // GET student by ID
    @GetMapping("/{id}")
    public ResponseEntity<?> getStudentById(@PathVariable Long id) {
        Optional<Student> student = studentRepository.findById(id);
        if (student.isPresent()) {
            return ResponseEntity.ok(student.get());
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(createErrorResponse("Ã‰tudiant non trouvÃ©"));
        }
    }

    // GET student by student code
    @GetMapping("/code/{studentCode}")
    public ResponseEntity<?> getStudentByCode(@PathVariable String studentCode) {
        Optional<Student> student = studentRepository.findByStudentCode(studentCode);
        if (student.isPresent()) {
            return ResponseEntity.ok(student.get());
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(createErrorResponse("Ã‰tudiant non trouvÃ© avec le code: " + studentCode));
        }
    }

    // GET student by QR code
    @GetMapping("/qr/{qrCode}")
    public ResponseEntity<?> getStudentByQrCode(@PathVariable String qrCode) {
        Optional<Student> student = studentRepository.findByQrCode(qrCode);
        if (student.isPresent()) {
            return ResponseEntity.ok(student.get());
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(createErrorResponse("Ã‰tudiant non trouvÃ© avec le QR code: " + qrCode));
        }
    }

    // GET students by class group
    @GetMapping("/class/{classGroup}")
    public List<Student> getStudentsByClass(@PathVariable String classGroup) {
        return studentRepository.findByClassGroupOrderByFullNameAsc(classGroup);
    }

    // GET students with pending returns
    @GetMapping("/pending-returns")
    public List<Student> getStudentsWithPendingReturns() {
        return studentRepository.findStudentsWithPendingReturns();
    }

    // GET students with overdue items
    @GetMapping("/overdue")
    public List<Student> getStudentsWithOverdueItems() {
        return studentRepository.findStudentsWithOverdueItems(LocalDateTime.now());
    }

    // Search students
    @GetMapping("/search")
    public List<Student> searchStudents(@RequestParam(required = false) String searchTerm,
                                        @RequestParam(required = false) String classGroup,
                                        @RequestParam(required = false) String academicYear,
                                        @RequestParam(required = false) String status) {

        if (searchTerm != null && !searchTerm.trim().isEmpty()) {
            return studentRepository.findByNameOrCode(searchTerm.trim());
        }

        Student.StudentStatus studentStatus = null;
        if (status != null) {
            try {
                studentStatus = Student.StudentStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException e) {
                // Invalid status, ignore
            }
        }

        return studentRepository.findByMultipleCriteria(null, null, classGroup,
                academicYear, studentStatus);
    }

    // GET student transactions
    @GetMapping("/{id}/transactions")
    public List<Transaction> getStudentTransactions(@PathVariable Long id,
                                                    @RequestParam(required = false) String startDate,
                                                    @RequestParam(required = false) String endDate) {
        Optional<Student> student = studentRepository.findById(id);
        if (!student.isPresent()) {
            return List.of();
        }

        if (startDate != null && endDate != null) {
            LocalDateTime start = LocalDateTime.parse(startDate);
            LocalDateTime end = LocalDateTime.parse(endDate);
            return transactionRepository.findByStudentAndDateRange(student.get(), start, end);
        } else {
            return transactionRepository.findByStudent(student.get());
        }
    }

    // GET student active checkouts
    // Manual fix for active-checkouts endpoint in StudentController

    // A simplified, error-handling version of the active checkouts endpoint for StudentController.java

    @GetMapping("/{id}/active-checkouts")
    public ResponseEntity<?> getStudentActiveCheckouts(@PathVariable Long id) {
        try {
            Optional<Student> student = studentRepository.findById(id);
            if (!student.isPresent()) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Ã‰tudiant non trouvÃ©");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
            }

            // Instead of using a custom query, use findAll and filter manually
            List<Transaction> allTransactions = transactionRepository.findAll();

            // Filter to find active checkouts for this student
            List<Map<String, Object>> result = new ArrayList<>();
            for (Transaction t : allTransactions) {
                // Check if this is a CHECKOUT transaction with PENDING status for this student
                if (t.getTransactionType() == Transaction.TransactionType.CHECKOUT &&
                        t.getStatus() == Transaction.TransactionStatus.PENDING &&
                        t.getStudent() != null &&
                        t.getStudent().getId().equals(student.get().getId())) {

                    // Create a simplified transaction object
                    Map<String, Object> transaction = new HashMap<>();
                    transaction.put("id", t.getId());
                    transaction.put("quantity", t.getQuantity());
                    transaction.put("transactionType", t.getTransactionType().toString());
                    transaction.put("status", t.getStatus().toString());
                    transaction.put("transactionDate", t.getTransactionDate().toString());

                    // Include piece information
                    if (t.getPiece() != null) {
                        Map<String, Object> piece = new HashMap<>();
                        piece.put("id", t.getPiece().getId());
                        piece.put("name", t.getPiece().getName());
                        piece.put("category", t.getPiece().getCategory());
                        transaction.put("piece", piece);
                    }

                    result.add(transaction);
                }
            }

            // Return the results
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            e.printStackTrace(); // Log the stack trace
            Map<String, String> error = new HashMap<>();
            error.put("error", "Error retrieving active checkouts: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @GetMapping("/student/{studentCode}/history")
    public ResponseEntity<?> getStudentTransactionHistory(
            @PathVariable String studentCode,
            @RequestParam(defaultValue = "30") int days) {

        try {
            // Find student by code
            Optional<Student> student = studentRepository.findByStudentCode(studentCode);
            if (!student.isPresent()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Student not found"));
            }

            // Calculate date range
            LocalDateTime since = LocalDateTime.now().minusDays(days);

            // Get transactions
            List<Transaction> transactions = transactionRepository.findByStudentAndDateRange(
                    student.get(), since, LocalDateTime.now());

            // Format response with necessary details
            Map<String, Object> response = new HashMap<>();
            response.put("student", Map.of(
                    "id", student.get().getId(),
                    "fullName", student.get().getFullName(),
                    "studentCode", student.get().getStudentCode()
            ));
            response.put("history", transactions); // This is the key field expected by frontend
            response.put("totalTransactions", transactions.size());
            response.put("period", days + " days");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error retrieving transaction history: " + e.getMessage()));
        }
    }


    // PUT - update existing student
    @PutMapping("/{id}")
    public ResponseEntity<?> updateStudent(@PathVariable Long id, @RequestBody StudentUpdateRequest request) {
        Optional<Student> existingStudent = studentRepository.findById(id);

        if (!existingStudent.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(createErrorResponse("Ã‰tudiant non trouvÃ©"));
        }

        try {
            Student student = existingStudent.get();

            if (request.getFullName() != null && !request.getFullName().trim().isEmpty()) {
                student.setFullName(request.getFullName());
            }

            if (request.getStudentCode() != null &&
                    !request.getStudentCode().equals(student.getStudentCode())) {
                if (studentRepository.existsByStudentCode(request.getStudentCode())) {
                    return ResponseEntity.badRequest()
                            .body(createErrorResponse("Ce code Ã©tudiant existe dÃ©jÃ "));
                }
                student.setStudentCode(request.getStudentCode());
            }

            if (request.getEmail() != null &&
                    !request.getEmail().equals(student.getEmail())) {
                if (studentRepository.existsByEmail(request.getEmail())) {
                    return ResponseEntity.badRequest()
                            .body(createErrorResponse("Cette adresse email est dÃ©jÃ  utilisÃ©e"));
                }
                student.setEmail(request.getEmail());
            }

            if (request.getPhoneNumber() != null) {
                student.setPhoneNumber(request.getPhoneNumber());
            }

            if (request.getClassGroup() != null) {
                student.setClassGroup(request.getClassGroup());
            }

            if (request.getAcademicYear() != null) {
                student.setAcademicYear(request.getAcademicYear());
            }

            if (request.getStatus() != null) {
                student.setStatus(request.getStatus());
            }

            Student updatedStudent = studentRepository.save(student);
            return ResponseEntity.ok(updatedStudent);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Erreur lors de la mise Ã  jour: " + e.getMessage()));
        }
    }


    // GET statistics
    @GetMapping("/statistics")
    public ResponseEntity<?> getStatistics() {
        try {
            Map<String, Object> statistics = new HashMap<>();

            statistics.put("totalStudents", studentRepository.count());
            statistics.put("activeStudents", studentRepository.countByStatus(Student.StudentStatus.ACTIVE));
            statistics.put("studentsWithPendingReturns", studentRepository.findStudentsWithPendingReturns().size());
            statistics.put("studentsWithOverdueItems", studentRepository.findStudentsWithOverdueItems(LocalDateTime.now()).size());
            statistics.put("allClassGroups", studentRepository.findAllClassGroups());
            statistics.put("allAcademicYears", studentRepository.findAllAcademicYears());

            return ResponseEntity.ok(statistics);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Erreur lors du calcul des statistiques: " + e.getMessage()));
        }
    }

    // GET all class groups
    @GetMapping("/class-groups")
    public List<String> getAllClassGroups() {
        return studentRepository.findAllClassGroups();
    }

    // GET all academic years
    @GetMapping("/academic-years")
    public List<String> getAllAcademicYears() {
        return studentRepository.findAllAcademicYears();
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
    public static class StudentRequest {
        private String fullName;
        private String studentCode;
        private String email;
        private String phoneNumber;
        private String classGroup;
        private String academicYear;
        private Student.StudentStatus status;

        // Getters and setters
        public String getFullName() { return fullName; }
        public void setFullName(String fullName) { this.fullName = fullName; }

        public String getStudentCode() { return studentCode; }
        public void setStudentCode(String studentCode) { this.studentCode = studentCode; }

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }

        public String getPhoneNumber() { return phoneNumber; }
        public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

        public String getClassGroup() { return classGroup; }
        public void setClassGroup(String classGroup) { this.classGroup = classGroup; }

        public String getAcademicYear() { return academicYear; }
        public void setAcademicYear(String academicYear) { this.academicYear = academicYear; }

        public Student.StudentStatus getStatus() { return status; }
        public void setStatus(Student.StudentStatus status) { this.status = status; }
    }

    public static class StudentUpdateRequest {
        private String fullName;
        private String studentCode;
        private String email;
        private String phoneNumber;
        private String classGroup;
        private String academicYear;
        private Student.StudentStatus status;

        // Getters and setters
        public String getFullName() { return fullName; }
        public void setFullName(String fullName) { this.fullName = fullName; }

        public String getStudentCode() { return studentCode; }
        public void setStudentCode(String studentCode) { this.studentCode = studentCode; }

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }

        public String getPhoneNumber() { return phoneNumber; }
        public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

        public String getClassGroup() { return classGroup; }
        public void setClassGroup(String classGroup) { this.classGroup = classGroup; }

        public String getAcademicYear() { return academicYear; }
        public void setAcademicYear(String academicYear) { this.academicYear = academicYear; }

        public Student.StudentStatus getStatus() { return status; }
        public void setStatus(Student.StudentStatus status) { this.status = status; }
    }

    @GetMapping("/validate-qr")
    public ResponseEntity<?> validateQr(@RequestParam String qrData) {
        // Debug log the QR data
        System.out.println("Received QR data: " + qrData);

        Optional<Student> student = studentRepository.findByQrCodeData(qrData);
        // Debug log the search result
        System.out.println("Student found: " + (student.isPresent() ? "yes" : "no"));

        if (student.isPresent()) {
            // Update last activity
            Student foundStudent = student.get();
            foundStudent.updateActivity();
            studentRepository.save(foundStudent);

            // Return student info
            Map<String, Object> response = new HashMap<>();
            response.put("id", foundStudent.getId());
            response.put("fullName", foundStudent.getFullName());
            response.put("studentCode", foundStudent.getStudentCode());
            response.put("status", foundStudent.getStatus());
            response.put("classGroup", foundStudent.getClassGroup());
            response.put("academicYear", foundStudent.getAcademicYear());

            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(createErrorResponse("QR code non valide"));
        }
    }

    // Get QR code image
    @GetMapping("/qr-image/{id}")
    public ResponseEntity<?> getQrCodeImage(@PathVariable Long id) {
        Optional<Student> student = studentRepository.findById(id);
        if (student.isPresent() && student.get().getQrCodePath() != null) {
            try {
                File qrFile = new File(student.get().getQrCodePath());
                if (qrFile.exists()) {
                    byte[] imageBytes = Files.readAllBytes(Paths.get(student.get().getQrCodePath()));
                    return ResponseEntity.ok()
                            .contentType(MediaType.IMAGE_PNG)
                            .body(imageBytes);
                }
            } catch (IOException e) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(createErrorResponse("Erreur lors de la rÃ©cupÃ©ration de l'image QR code"));
            }
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(createErrorResponse("QR code non trouvÃ©"));
    }

    // Update existing POST method for student creation to include QR code generation
    // Update the createStudent method in StudentController
    @PostMapping
    public ResponseEntity<?> createStudent(@RequestBody StudentRequest request) {
        try {
            // Validate required fields
            if (request.getFullName() == null || request.getFullName().trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("Le nom complet est requis"));
            }

            if (request.getStudentCode() == null || request.getStudentCode().trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("Le code Ã©tudiant est requis"));
            }

            if (studentRepository.existsByStudentCode(request.getStudentCode())) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("Ce code Ã©tudiant existe dÃ©jÃ "));
            }

            if (request.getEmail() != null && studentRepository.existsByEmail(request.getEmail())) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("Cette adresse email est dÃ©jÃ  utilisÃ©e"));
            }

            // 1. Create student object
            Student student = new Student();
            student.setFullName(request.getFullName());
            student.setStudentCode(request.getStudentCode());
            student.setEmail(request.getEmail());
            student.setPhoneNumber(request.getPhoneNumber());
            student.setClassGroup(request.getClassGroup());
            student.setAcademicYear(request.getAcademicYear());
            student.setStatus(request.getStatus() != null ? request.getStatus() : Student.StudentStatus.ACTIVE);

            // 2. Save initial student to get ID
            Student savedStudent = studentRepository.save(student);

            // 3. Generate unique QR data
            String qrCodeData = qrCodeService.generateQrCodeData(savedStudent.getId());
            savedStudent.setQrCodeData(qrCodeData);

            // 4. Generate QR code image
            byte[] qrCodeImage = qrCodeService.generateQrCode(qrCodeData, 250, 250);

            // 5. Save QR image to disk with student name
            String qrCodePath = qrCodeService.saveQrCodeToDisk(
                    qrCodeImage,
                    savedStudent.getId().toString(),
                    savedStudent.getFullName()
            );
            savedStudent.setQrCodePath(qrCodePath);

            // 6. Update student with QR info
            savedStudent = studentRepository.save(savedStudent);

            // 7. Return student with QR info
            Map<String, Object> response = new HashMap<>();
            response.put("student", savedStudent);
            response.put("qrCodeUrl", "/api/students/qr-image/" + savedStudent.getId());
            response.put("message", "Ã‰tudiant crÃ©Ã© avec succÃ¨s avec QR code");

            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Erreur lors de la crÃ©ation: " + e.getMessage()));
        }
    }

    // Update the regenerate-qr method
    @PostMapping("/{id}/regenerate-qr")
    public ResponseEntity<?> regenerateQrCode(@PathVariable Long id) {
        Optional<Student> existingStudent = studentRepository.findById(id);

        if (!existingStudent.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(createErrorResponse("Ã‰tudiant non trouvÃ©"));
        }

        try {
            Student student = existingStudent.get();

            // Generate new QR code data
            String qrCodeData = qrCodeService.generateQrCodeData(student.getId());
            student.setQrCodeData(qrCodeData);

            // Generate new QR code image
            byte[] qrCodeImage = qrCodeService.generateQrCode(qrCodeData, 250, 250);

            // Save to disk with student name
            String qrCodePath = qrCodeService.saveQrCodeToDisk(
                    qrCodeImage,
                    student.getId().toString(),
                    student.getFullName()
            );
            student.setQrCodePath(qrCodePath);

            // Update student
            Student updatedStudent = studentRepository.save(student);

            Map<String, Object> response = new HashMap<>();
            response.put("student", updatedStudent);
            response.put("qrCodeUrl", "/api/students/qr-image/" + updatedStudent.getId());
            response.put("message", "Nouveau QR code gÃ©nÃ©rÃ© avec succÃ¨s");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Erreur lors de la gÃ©nÃ©ration du QR code: " + e.getMessage()));
        }
    }

    // Update the regenerate-qr method for generating new QR codes


    // Update DELETE method to also delete QR code files
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteStudent(@PathVariable Long id) {
        if (!studentRepository.existsById(id)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(createErrorResponse("Ã‰tudiant non trouvÃ©"));
        }

        // Check if student has transactions
        Optional<Student> student = studentRepository.findById(id);
        List<Transaction> transactions = transactionRepository.findByStudent(student.get());

        if (!transactions.isEmpty()) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(createErrorResponse("Impossible de supprimer cet Ã©tudiant car il a des transactions associÃ©es"));
        }

        try {
            // Delete QR code file if exists
            if (student.get().getQrCodePath() != null) {
                try {
                    Files.deleteIfExists(Paths.get(student.get().getQrCodePath()));
                } catch (IOException e) {
                    // Log error but continue with student deletion
                    System.err.println("Error deleting QR code file: " + e.getMessage());
                }
            }

            studentRepository.deleteById(id);
            return ResponseEntity.ok(createSuccessResponse("Ã‰tudiant supprimÃ© avec succÃ¨s"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Erreur lors de la suppression: " + e.getMessage()));
        }
    }
}