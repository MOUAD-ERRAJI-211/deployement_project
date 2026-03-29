package orthoproconnect.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import orthoproconnect.model.Admin;
import orthoproconnect.model.Teacher;
import orthoproconnect.model.TeacherApproval;
import orthoproconnect.repository.AdminRepository;
import orthoproconnect.repository.TeacherApprovalRepository;
import orthoproconnect.repository.TeacherRepository;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/admins")
public class AdminController {

    @Autowired
    private AdminRepository adminRepository;

    @Autowired
    private TeacherRepository teacherRepository;

    @Autowired
    private TeacherApprovalRepository approvalRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // GET all admins
    @GetMapping
    public List<Admin> getAllAdmins() {
        return adminRepository.findAll();
    }

    // GET admin by ID
    @GetMapping("/{id}")
    public ResponseEntity<?> getAdminById(@PathVariable Long id) {
        Optional<Admin> admin = adminRepository.findById(id);
        if (admin.isPresent()) {
            return ResponseEntity.ok(admin.get());
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Administrateur non trouvÃ©");
        }
    }

    // POST - create new admin (should be restricted to super admins in a real app)
    @PostMapping
    public ResponseEntity<?> createAdmin(@RequestBody AdminRequest request) {
        // Check if email already exists
        if (adminRepository.existsByEmail(request.getEmail())) {
            return ResponseEntity.badRequest().body("Cette adresse email est dÃ©jÃ  utilisÃ©e");
        }

        try {
            Admin admin = new Admin();
            admin.setFirstname(request.getFirstname());
            admin.setLastname(request.getLastname());
            admin.setEmail(request.getEmail());
            admin.setPasswordHash(passwordEncoder.encode(request.getPassword()));

            Admin savedAdmin = adminRepository.save(admin);
            return ResponseEntity.status(HttpStatus.CREATED).body(savedAdmin);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Erreur lors de la crÃ©ation: " + e.getMessage());
        }
    }

    // PUT - update existing admin
    @PutMapping("/{id}")
    public ResponseEntity<?> updateAdmin(@PathVariable Long id, @RequestBody AdminUpdateRequest request) {
        Optional<Admin> existingAdmin = adminRepository.findById(id);

        if (!existingAdmin.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Administrateur non trouvÃ©");
        }

        Admin admin = existingAdmin.get();

        if (request.getFirstname() != null) {
            admin.setFirstname(request.getFirstname());
        }

        if (request.getLastname() != null) {
            admin.setLastname(request.getLastname());
        }

        if (request.getEmail() != null && !request.getEmail().equals(admin.getEmail())) {
            // Check if new email is already in use
            if (adminRepository.existsByEmail(request.getEmail())) {
                return ResponseEntity.badRequest().body("Cette adresse email est dÃ©jÃ  utilisÃ©e");
            }
            admin.setEmail(request.getEmail());
        }

        if (request.getPassword() != null && !request.getPassword().isEmpty()) {
            admin.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        }

        if (request.getProfileImageUrl() != null) {
            admin.setProfileImageUrl(request.getProfileImageUrl());
        }

        Admin updatedAdmin = adminRepository.save(admin);
        return ResponseEntity.ok(updatedAdmin);
    }

    // DELETE admin
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteAdmin(@PathVariable Long id) {
        if (!adminRepository.existsById(id)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Administrateur non trouvÃ©");
        }

        adminRepository.deleteById(id);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Administrateur supprimÃ© avec succÃ¨s");
        return ResponseEntity.ok(response);
    }

    // Login endpoint
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        Optional<Admin> admin = adminRepository.findByEmail(request.getEmail());

        if (!admin.isPresent()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Email ou mot de passe incorrect");
        }

        if (!passwordEncoder.matches(request.getPassword(), admin.get().getPasswordHash())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Email ou mot de passe incorrect");
        }

        // Update last login time
        Admin loggedInAdmin = admin.get();
        loggedInAdmin.setLastLogin(LocalDateTime.now());
        adminRepository.save(loggedInAdmin);

        // For a real application, you would generate a JWT token here
        Map<String, Object> response = new HashMap<>();
        response.put("id", loggedInAdmin.getId());
        response.put("firstname", loggedInAdmin.getFirstname());
        response.put("lastname", loggedInAdmin.getLastname());
        response.put("email", loggedInAdmin.getEmail());

        return ResponseEntity.ok(response);
    }

    // Teacher approval endpoints
    @GetMapping("/pending-teachers")
    public List<Teacher> getPendingTeachers() {
        return teacherRepository.findByIsApproved(false);
    }

    @PostMapping("/approve-teacher/{teacherId}")
    public ResponseEntity<?> approveTeacher(
            @PathVariable Long teacherId,
            @RequestBody ApprovalRequest request,
            @RequestHeader("Authorization") String authHeader) {

        // In a real app, extract admin ID from JWT token
        // For demo purposes, we'll use the adminId from the request
        Optional<Admin> admin = adminRepository.findById(request.getAdminId());
        Optional<Teacher> teacher = teacherRepository.findById(teacherId);

        if (!admin.isPresent()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Admin non authentifiÃ©");
        }

        if (!teacher.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Enseignant non trouvÃ©");
        }

        // Update teacher approval status
        Teacher teacherToApprove = teacher.get();
        teacherToApprove.setApproved(true);
        teacherRepository.save(teacherToApprove);

        // Create approval record
        TeacherApproval approval = new TeacherApproval(
                teacherToApprove,
                admin.get(),
                TeacherApproval.STATUS_APPROVED,
                request.getComments()
        );
        approvalRepository.save(approval);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Compte enseignant approuvÃ© avec succÃ¨s");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/reject-teacher/{teacherId}")
    public ResponseEntity<?> rejectTeacher(
            @PathVariable Long teacherId,
            @RequestBody ApprovalRequest request,
            @RequestHeader("Authorization") String authHeader) {

        // In a real app, extract admin ID from JWT token
        Optional<Admin> admin = adminRepository.findById(request.getAdminId());
        Optional<Teacher> teacher = teacherRepository.findById(teacherId);

        if (!admin.isPresent()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Admin non authentifiÃ©");
        }

        if (!teacher.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Enseignant non trouvÃ©");
        }

        // Update teacher approval status (keeping as false)
        Teacher teacherToReject = teacher.get();

        // Create rejection record
        TeacherApproval rejection = new TeacherApproval(
                teacherToReject,
                admin.get(),
                TeacherApproval.STATUS_REJECTED,
                request.getComments()
        );
        approvalRepository.save(rejection);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Compte enseignant rejetÃ©");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/teacher-approvals")
    public List<TeacherApproval> getAllApprovals() {
        return approvalRepository.findAll();
    }

    @GetMapping("/teacher-approvals/{teacherId}")
    public List<TeacherApproval> getTeacherApprovals(@PathVariable Long teacherId) {
        Optional<Teacher> teacher = teacherRepository.findById(teacherId);
        if (teacher.isPresent()) {
            return approvalRepository.findByTeacher(teacher.get());
        } else {
            return List.of();
        }
    }

    // Request classes
    public static class AdminRequest {
        private String firstname;
        private String lastname;
        private String email;
        private String password;

        public String getFirstname() {
            return firstname;
        }

        public void setFirstname(String firstname) {
            this.firstname = firstname;
        }

        public String getLastname() {
            return lastname;
        }

        public void setLastname(String lastname) {
            this.lastname = lastname;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }

    public static class AdminUpdateRequest {
        private String firstname;
        private String lastname;
        private String email;
        private String password;
        private String profileImageUrl;

        public String getFirstname() {
            return firstname;
        }

        public void setFirstname(String firstname) {
            this.firstname = firstname;
        }

        public String getLastname() {
            return lastname;
        }

        public void setLastname(String lastname) {
            this.lastname = lastname;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getProfileImageUrl() {
            return profileImageUrl;
        }

        public void setProfileImageUrl(String profileImageUrl) {
            this.profileImageUrl = profileImageUrl;
        }
    }

    public static class LoginRequest {
        private String email;
        private String password;

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }

    public static class ApprovalRequest {
        private Long adminId; // In a real app, this would be extracted from JWT token
        private String comments;

        public Long getAdminId() {
            return adminId;
        }

        public void setAdminId(Long adminId) {
            this.adminId = adminId;
        }

        public String getComments() {
            return comments;
        }

        public void setComments(String comments) {
            this.comments = comments;
        }
    }
}