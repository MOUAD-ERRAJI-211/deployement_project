package orthoproconnect.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import orthoproconnect.model.Teacher;
import orthoproconnect.model.Model3D;
import orthoproconnect.repository.TeacherRepository;
import orthoproconnect.repository.Model3DRepository;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/teachers")
public class TeacherController {

    @Autowired
    private TeacherRepository teacherRepository;

    @Autowired
    private Model3DRepository model3DRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // GET all teachers
    @GetMapping
    public List<Teacher> getAllTeachers() {
        return teacherRepository.findAll();
    }

    // GET teacher by ID
    @GetMapping("/{id}")
    public ResponseEntity<?> getTeacherById(@PathVariable Long id) {
        Optional<Teacher> teacher = teacherRepository.findById(id);
        if (teacher.isPresent()) {
            return ResponseEntity.ok(teacher.get());
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Enseignant non trouvÃ©");
        }
    }

    // GET pending teachers
    @GetMapping("/pending")
    public List<Teacher> getPendingTeachers() {
        return teacherRepository.findByIsApproved(false);
    }

    // GET approved teachers
    @GetMapping("/approved")
    public List<Teacher> getApprovedTeachers() {
        return teacherRepository.findByIsApproved(true);
    }

    // GET teachers by institution
    @GetMapping("/institution/{institution}")
    public List<Teacher> getTeachersByInstitution(@PathVariable String institution) {
        return teacherRepository.findByInstitution(institution);
    }

    // POST - create new teacher
    @PostMapping
    public ResponseEntity<?> createTeacher(@RequestBody TeacherRequest request) {
        // Check if email already exists
        if (teacherRepository.existsByEmail(request.getEmail())) {
            return ResponseEntity.badRequest().body("Cette adresse email est dÃ©jÃ  utilisÃ©e");
        }

        try {
            Teacher teacher = new Teacher();
            teacher.setFirstname(request.getFirstname());
            teacher.setLastname(request.getLastname());
            teacher.setEmail(request.getEmail());
            teacher.setPasswordHash(passwordEncoder.encode(request.getPassword()));
            teacher.setInstitution(request.getInstitution());
            teacher.setApproved(false); // New teachers are not approved by default

            Teacher savedTeacher = teacherRepository.save(teacher);
            return ResponseEntity.status(HttpStatus.CREATED).body(savedTeacher);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Erreur lors de la crÃ©ation: " + e.getMessage());
        }
    }

    // PUT - update existing teacher
    @PutMapping("/{id}")
    public ResponseEntity<?> updateTeacher(@PathVariable Long id, @RequestBody TeacherUpdateRequest request) {
        Optional<Teacher> existingTeacher = teacherRepository.findById(id);

        if (!existingTeacher.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Enseignant non trouvÃ©");
        }

        Teacher teacher = existingTeacher.get();

        if (request.getFirstname() != null) {
            teacher.setFirstname(request.getFirstname());
        }

        if (request.getLastname() != null) {
            teacher.setLastname(request.getLastname());
        }

        if (request.getEmail() != null && !request.getEmail().equals(teacher.getEmail())) {
            // Check if new email is already in use
            if (teacherRepository.existsByEmail(request.getEmail())) {
                return ResponseEntity.badRequest().body("Cette adresse email est dÃ©jÃ  utilisÃ©e");
            }
            teacher.setEmail(request.getEmail());
        }

        if (request.getPassword() != null && !request.getPassword().isEmpty()) {
            teacher.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        }

        if (request.getInstitution() != null) {
            teacher.setInstitution(request.getInstitution());
        }

        if (request.getProfileImageUrl() != null) {
            teacher.setProfileImageUrl(request.getProfileImageUrl());
        }

        // Only admins should be able to change approval status
        if (request.getIsApproved() != null) {
            teacher.setApproved(request.getIsApproved());
        }

        Teacher updatedTeacher = teacherRepository.save(teacher);
        return ResponseEntity.ok(updatedTeacher);
    }

    // DELETE teacher
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteTeacher(@PathVariable Long id) {
        if (!teacherRepository.existsById(id)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Enseignant non trouvÃ©");
        }

        // Check if teacher has created models
        List<Model3D> teachersModels = model3DRepository.findByCreatedBy(teacherRepository.findById(id).get());
        if (!teachersModels.isEmpty()) {
            // Either delete models or handle differently
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Impossible de supprimer cet enseignant car il a crÃ©Ã© " +
                    teachersModels.size() + " modÃ¨les 3D");
        }

        teacherRepository.deleteById(id);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Enseignant supprimÃ© avec succÃ¨s");
        return ResponseEntity.ok(response);
    }

    // Login endpoint
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        Optional<Teacher> teacher = teacherRepository.findByEmail(request.getEmail());

        if (!teacher.isPresent()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Email ou mot de passe incorrect");
        }

        if (!passwordEncoder.matches(request.getPassword(), teacher.get().getPasswordHash())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Email ou mot de passe incorrect");
        }

        // Check if teacher account is approved
        if (!teacher.get().isApproved()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Votre compte n'a pas encore Ã©tÃ© approuvÃ© par un administrateur");
        }

        // Update last login time
        Teacher loggedInTeacher = teacher.get();
        loggedInTeacher.setLastLogin(LocalDateTime.now());
        teacherRepository.save(loggedInTeacher);

        // For a real application, you would generate a JWT token here
        // But for now, we'll use a simple token format that includes the ID
        String token = loggedInTeacher.getId().toString(); // Using the ID as the token

        Map<String, Object> response = new HashMap<>();
        response.put("id", loggedInTeacher.getId());
        response.put("firstname", loggedInTeacher.getFirstname());
        response.put("lastname", loggedInTeacher.getLastname());
        response.put("email", loggedInTeacher.getEmail());
        response.put("institution", loggedInTeacher.getInstitution());
        response.put("token", token); // Include the token in the response

        return ResponseEntity.ok(response);
    }

    // Validate token and get current teacher
    @GetMapping("/current")
    public ResponseEntity<?> getCurrentTeacher(@RequestHeader("Authorization") String authHeader) {
        try {
            // In a real app, this would verify the JWT token and extract the teacher ID
            Long teacherId = extractTeacherId(authHeader);

            if (teacherId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Token invalide");
            }

            Optional<Teacher> teacher = teacherRepository.findById(teacherId);
            if (!teacher.isPresent() || !teacher.get().isApproved()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Non autorisÃ©");
            }

            return ResponseEntity.ok(teacher.get());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Token invalide");
        }
    }

    // Helper method to extract teacher ID from auth header (demo)
    private Long extractTeacherId(String authHeader) {
        // In a real app, this would extract the teacher ID from the JWT token
        // For demo, we'll just check if the header exists and contains a valid teacher ID
        if (authHeader == null || authHeader.isEmpty()) {
            return null;
        }

        try {
            // Format: "Bearer TEACHER_ID" (simplified for demo)
            String[] parts = authHeader.split(" ");
            if (parts.length != 2 || !parts[0].equals("Bearer")) {
                return null;
            }

            return Long.parseLong(parts[1]);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // Request classes
    public static class TeacherRequest {
        private String firstname;
        private String lastname;
        private String email;
        private String password;
        private String institution;

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

        public String getInstitution() {
            return institution;
        }

        public void setInstitution(String institution) {
            this.institution = institution;
        }
    }

    public static class TeacherUpdateRequest {
        private String firstname;
        private String lastname;
        private String email;
        private String password;
        private String institution;
        private String profileImageUrl;
        private Boolean isApproved;

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

        public String getInstitution() {
            return institution;
        }

        public void setInstitution(String institution) {
            this.institution = institution;
        }

        public String getProfileImageUrl() {
            return profileImageUrl;
        }

        public void setProfileImageUrl(String profileImageUrl) {
            this.profileImageUrl = profileImageUrl;
        }

        public Boolean getIsApproved() {
            return isApproved;
        }

        public void setIsApproved(Boolean isApproved) {
            this.isApproved = isApproved;
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
}