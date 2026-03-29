package orthoproconnect.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import orthoproconnect.model.Model3D;
import orthoproconnect.model.Teacher;
import orthoproconnect.repository.Model3DRepository;
import orthoproconnect.repository.TeacherRepository;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/models")
public class Model3DController {

    @Autowired
    private Model3DRepository model3DRepository;

    @Autowired
    private TeacherRepository teacherRepository;

    @Value("${app.models-dir:uploads/models}")
    private String uploadDir;

    // Get all models (admin only or for authorized teachers)
    @GetMapping
    public ResponseEntity<?> getAllModels(@RequestHeader("Authorization") String authHeader) {
        try {
            Long teacherId = extractTeacherId(authHeader);
            if (teacherId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Non autorisÃ©");
            }

            Optional<Teacher> teacher = teacherRepository.findById(teacherId);
            if (!teacher.isPresent() || !teacher.get().isApproved()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Non autorisÃ©");
            }

            List<Model3D> models = model3DRepository.findAll();
            return ResponseEntity.ok(models);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur lors de la rÃ©cupÃ©ration des modÃ¨les: " + e.getMessage());
        }
    }

    // Get public models
    @GetMapping("/public")
    public ResponseEntity<?> getPublicModels() {
        try {
            List<Model3D> publicModels = model3DRepository.findByIsPublicTrue();
            return ResponseEntity.ok(publicModels);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur lors de la rÃ©cupÃ©ration des modÃ¨les publics: " + e.getMessage());
        }
    }

    // Get teacher's own models
    @GetMapping("/my-models")
    public ResponseEntity<?> getMyModels(@RequestHeader("Authorization") String authHeader) {
        try {
            Long teacherId = extractTeacherId(authHeader);
            if (teacherId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Non autorisÃ©");
            }

            Optional<Teacher> teacher = teacherRepository.findById(teacherId);
            if (!teacher.isPresent() || !teacher.get().isApproved()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Non autorisÃ©");
            }

            List<Model3D> teacherModels = model3DRepository.findByCreatedBy(teacher.get());
            return ResponseEntity.ok(teacherModels);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur lors de la rÃ©cupÃ©ration des modÃ¨les: " + e.getMessage());
        }
    }

    // Get teacher's private models
    @GetMapping("/my-models/private")
    public ResponseEntity<?> getMyPrivateModels(@RequestHeader("Authorization") String authHeader) {
        try {
            Long teacherId = extractTeacherId(authHeader);
            if (teacherId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Non autorisÃ©");
            }

            Optional<Teacher> teacher = teacherRepository.findById(teacherId);
            if (!teacher.isPresent() || !teacher.get().isApproved()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Non autorisÃ©");
            }

            List<Model3D> privateModels = model3DRepository.findByCreatedByAndIsPublicFalse(teacher.get());
            return ResponseEntity.ok(privateModels);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur lors de la rÃ©cupÃ©ration des modÃ¨les privÃ©s: " + e.getMessage());
        }
    }

    // Get model by ID
    @GetMapping("/{id}")
    public ResponseEntity<?> getModelById(@PathVariable Long id, @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            System.out.println("Fetching model with ID: " + id);

            Optional<Model3D> model = model3DRepository.findById(id);
            if (!model.isPresent()) {
                System.out.println("Model not found in database with ID: " + id);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("ModÃ¨le non trouvÃ©");
            }

            // Log the model details
            System.out.println("Model found: " + model.get().getName());
            System.out.println("File path: " + model.get().getFilePath());
            System.out.println("File exists: " + Files.exists(Paths.get(model.get().getFilePath())));

            // If model is private, verify teacher
            if (!model.get().isPublic()) {
                Long teacherId = extractTeacherId(authHeader);
                if (teacherId == null) {
                    System.out.println("Unauthorized access: No teacher ID in token");
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Non autorisÃ©");
                }

                Optional<Teacher> teacher = teacherRepository.findById(teacherId);
                if (!teacher.isPresent() || !teacher.get().isApproved()) {
                    System.out.println("Unauthorized access: Teacher not found or not approved");
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Non autorisÃ©");
                }

                // Check if the teacher is the owner of the model
                if (!model.get().getCreatedBy().getId().equals(teacherId)) {
                    System.out.println("Forbidden access: Teacher " + teacherId +
                            " is not the owner of model created by " +
                            model.get().getCreatedBy().getId());
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Ce modÃ¨le est privÃ©");
                }
            }

            return ResponseEntity.ok(model.get());
        } catch (Exception e) {
            System.out.println("Error retrieving model: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur lors de la rÃ©cupÃ©ration du modÃ¨le: " + e.getMessage());
        }
    }



    // Upload a new model
    /**
     * Update the uploadModel method in Model3DController.java to handle paths correctly
     */
    @PostMapping
    public ResponseEntity<?> uploadModel(
            @RequestParam("file") MultipartFile file,
            @RequestParam("name") String name,
            @RequestParam("category") String category,
            @RequestParam("description") String description,
            @RequestParam("isPublic") boolean isPublic,
            @RequestHeader("Authorization") String authHeader) {
        try {
            Long teacherId = extractTeacherId(authHeader);
            System.out.println("Upload requested by teacher ID: " + teacherId);

            if (teacherId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Non autorisÃ©: Impossible d'identifier l'enseignant");
            }

            Optional<Teacher> teacher = teacherRepository.findById(teacherId);
            if (!teacher.isPresent() || !teacher.get().isApproved()) {
                System.out.println("Teacher not found or not approved: " + teacherId);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Non autorisÃ©");
            }

            // Validate file
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body("Fichier vide");
            }

            // Check if it's an STL file
            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null || (!originalFilename.toLowerCase().endsWith(".stl"))) {
                return ResponseEntity.badRequest().body("Seuls les fichiers STL sont acceptÃ©s");
            }

            System.out.println("Original filename: " + originalFilename);

            // Get the configured upload directory and create it if needed
            Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
            System.out.println("Upload directory (absolute): " + uploadPath);

            if (!Files.exists(uploadPath)) {
                System.out.println("Creating upload directory: " + uploadPath);
                Files.createDirectories(uploadPath);
            }

            // Generate unique filename
            String uniqueFilename = System.currentTimeMillis() + "_" + originalFilename;
            Path filePath = uploadPath.resolve(uniqueFilename);
            System.out.println("File will be saved to: " + filePath);

            // Save file
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
            System.out.println("File saved successfully");
            System.out.println("File exists after save: " + Files.exists(filePath));

            // Create model entity - store ONLY the filename, not the full path
            Model3D model = new Model3D();
            model.setName(name);
            model.setFilename(uniqueFilename); // Just the filename
            model.setFilePath(filePath.toString()); // Store full path for internal use
            model.setDescription(description);
            model.setCategory(category);
            model.setFileSize(file.getSize());
            model.setPublic(isPublic);
            model.setCreatedBy(teacher.get());

            Model3D savedModel = model3DRepository.save(model);
            System.out.println("Model saved to database with ID: " + savedModel.getId());

            return ResponseEntity.status(HttpStatus.CREATED).body(savedModel);
        } catch (IOException e) {
            System.out.println("IO Exception during file upload: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur lors de l'upload du fichier: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("General exception during model creation: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur lors de la crÃ©ation du modÃ¨le: " + e.getMessage());
        }
    }

    // Update model
    @PutMapping("/{id}")
    public ResponseEntity<?> updateModel(
            @PathVariable Long id,
            @RequestBody Map<String, Object> updates,
            @RequestHeader("Authorization") String authHeader) {
        try {
            Long teacherId = extractTeacherId(authHeader);
            if (teacherId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Non autorisÃ©");
            }

            Optional<Teacher> teacher = teacherRepository.findById(teacherId);
            if (!teacher.isPresent() || !teacher.get().isApproved()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Non autorisÃ©");
            }

            Optional<Model3D> existingModel = model3DRepository.findById(id);
            if (!existingModel.isPresent()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("ModÃ¨le non trouvÃ©");
            }

            // Check if the teacher is the owner of the model
            if (!existingModel.get().getCreatedBy().getId().equals(teacherId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Vous n'Ãªtes pas autorisÃ© Ã  modifier ce modÃ¨le");
            }

            Model3D model = existingModel.get();

            if (updates.containsKey("name")) {
                model.setName((String) updates.get("name"));
            }
            if (updates.containsKey("description")) {
                model.setDescription((String) updates.get("description"));
            }
            if (updates.containsKey("category")) {
                model.setCategory((String) updates.get("category"));
            }
            if (updates.containsKey("isPublic")) {
                model.setPublic((Boolean) updates.get("isPublic"));
            }

            Model3D updatedModel = model3DRepository.save(model);
            return ResponseEntity.ok(updatedModel);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur lors de la mise Ã  jour du modÃ¨le: " + e.getMessage());
        }
    }

    // Delete model
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteModel(@PathVariable Long id, @RequestHeader("Authorization") String authHeader) {
        try {
            Long teacherId = extractTeacherId(authHeader);
            if (teacherId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Non autorisÃ©");
            }

            Optional<Teacher> teacher = teacherRepository.findById(teacherId);
            if (!teacher.isPresent() || !teacher.get().isApproved()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Non autorisÃ©");
            }

            Optional<Model3D> model = model3DRepository.findById(id);
            if (!model.isPresent()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("ModÃ¨le non trouvÃ©");
            }

            // Check if the teacher is the owner of the model
            if (!model.get().getCreatedBy().getId().equals(teacherId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Vous n'Ãªtes pas autorisÃ© Ã  supprimer ce modÃ¨le");
            }

            // Delete file from storage
            Path filePath = Paths.get(model.get().getFilePath());
            Files.deleteIfExists(filePath);

            // Delete model from database
            model3DRepository.deleteById(id);

            Map<String, String> response = new HashMap<>();
            response.put("message", "ModÃ¨le supprimÃ© avec succÃ¨s");
            return ResponseEntity.ok(response);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur lors de la suppression du fichier: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur lors de la suppression du modÃ¨le: " + e.getMessage());
        }
    }

    // Download model
    /**
     * Update the download model endpoint in Model3DController.java
     */
    @GetMapping("/download/{id}")
    public ResponseEntity<?> downloadModel(@PathVariable Long id, @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            System.out.println("Download request for model ID: " + id);

            Optional<Model3D> modelOpt = model3DRepository.findById(id);
            if (!modelOpt.isPresent()) {
                System.out.println("Model not found in database with ID: " + id);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("ModÃ¨le non trouvÃ©");
            }

            Model3D model = modelOpt.get();
            System.out.println("Found model: " + model.getName());
            System.out.println("File path from DB: " + model.getFilePath());

            // If model is private, verify teacher
            if (!model.isPublic()) {
                Long teacherId = extractTeacherId(authHeader);
                if (teacherId == null) {
                    System.out.println("Unauthorized access: No teacher ID in token");
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Non autorisÃ©");
                }

                Optional<Teacher> teacher = teacherRepository.findById(teacherId);
                if (!teacher.isPresent() || !teacher.get().isApproved()) {
                    System.out.println("Unauthorized access: Teacher not found or not approved");
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Non autorisÃ©");
                }

                // Check if the teacher is the owner of the model
                if (!model.getCreatedBy().getId().equals(teacherId)) {
                    System.out.println("Forbidden access: Teacher " + teacherId +
                            " is not the owner of model created by " +
                            model.getCreatedBy().getId());
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Ce modÃ¨le est privÃ©");
                }
            }

            // Get file path
            Path filePath = Paths.get(model.getFilePath());
            System.out.println("Resolved file path: " + filePath);
            System.out.println("File exists: " + Files.exists(filePath));

            // If file doesn't exist at the exact path, try to find it in the models directory
            if (!Files.exists(filePath)) {
                System.out.println("File not found at exact path. Trying to find in models directory.");

                // Try to locate in upload directory using just the filename
                Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
                filePath = uploadPath.resolve(model.getFilename());

                System.out.println("Alternative path: " + filePath);
                System.out.println("File exists at alternative path: " + Files.exists(filePath));

                if (!Files.exists(filePath)) {
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Fichier non trouvÃ©");
                }
            }

            Resource resource;
            try {
                resource = new UrlResource(filePath.toUri());
            } catch (MalformedURLException e) {
                System.out.println("Malformed URL: " + e.getMessage());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("Erreur lors de la rÃ©cupÃ©ration du fichier: " + e.getMessage());
            }

            if (!resource.exists()) {
                System.out.println("Resource does not exist after URL resolution");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Fichier non trouvÃ©");
            }

            // Determine content type
            String contentType = "application/octet-stream";
            try {
                contentType = Files.probeContentType(filePath);
                if (contentType == null) {
                    contentType = "application/octet-stream";
                }
            } catch (IOException e) {
                System.out.println("Could not determine content type: " + e.getMessage());
                contentType = "application/octet-stream";
            }

            System.out.println("Serving file with content type: " + contentType);

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + model.getFilename() + "\"")
                    .body(resource);
        } catch (Exception e) {
            System.out.println("Error downloading model: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur lors du tÃ©lÃ©chargement du modÃ¨le: " + e.getMessage());
        }
    }


    // Debug endpoint - get model public state
    @GetMapping("/debug/{id}")
    public ResponseEntity<?> getModelPublicState(@PathVariable Long id) {
        try {
            Optional<Model3D> model = model3DRepository.findById(id);
            if (!model.isPresent()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Model not found");
            }

            Map<String, Object> response = new HashMap<>();
            response.put("id", model.get().getId());
            response.put("name", model.get().getName());
            response.put("isPublic", model.get().isPublic());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error: " + e.getMessage());
        }
    }

    // Get models by category
    @GetMapping("/category/{category}")
    public ResponseEntity<?> getModelsByCategory(@PathVariable String category, @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            List<Model3D> models;

            // If auth header is provided, verify teacher to include their private models
            if (authHeader != null && !authHeader.isEmpty()) {
                Long teacherId = extractTeacherId(authHeader);
                if (teacherId != null) {
                    Optional<Teacher> teacher = teacherRepository.findById(teacherId);
                    if (teacher.isPresent() && teacher.get().isApproved()) {
                        models = model3DRepository.findByCategory(category);
                        // Filter out private models not belonging to the current teacher
                        models = models.stream()
                                .filter(model -> model.isPublic() || model.getCreatedBy().getId().equals(teacherId))
                                .collect(Collectors.toList());
                        return ResponseEntity.ok(models);
                    }
                }
            }

            // If no auth or invalid auth, return only public models
            models = model3DRepository.findByCategoryAndIsPublicTrue(category);
            return ResponseEntity.ok(models);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur lors de la rÃ©cupÃ©ration des modÃ¨les: " + e.getMessage());
        }
    }

    private Long extractTeacherId(String authHeader) {
        System.out.println("Auth header received: " + (authHeader != null ? authHeader : "null"));

        if (authHeader == null || authHeader.isEmpty()) {
            return null;
        }

        try {
            // Format: "Bearer TOKEN"
            if (authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7); // Remove "Bearer " prefix
                System.out.println("Extracted token: " + token);

                // For the simple case where token is directly the teacher ID
                if (token.matches("\\d+")) {
                    Long teacherId = Long.parseLong(token);
                    System.out.println("Successfully extracted teacher ID: " + teacherId);
                    return teacherId;
                } else {
                    System.out.println("Token is not a numeric ID: " + token);
                    // You could implement JWT parsing here if needed
                }
            } else {
                System.out.println("Auth header doesn't start with 'Bearer '");
            }

            return null;
        } catch (Exception e) {
            System.out.println("Error extracting teacher ID: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Parse teacher ID from token - implement this method based on your token structure
     */
    /**
     * Parse teacher ID from token - implement this method based on your token structure
     */
    private Long parseTeacherIdFromToken(String token) {
        try {
            // If the token is directly the teacher ID (for your simplified auth)
            if (token.matches("\\d+")) {
                return Long.parseLong(token);
            }

            // If you're using JWT tokens, you should decode them here
            // This is a simplified example using a custom format "demo-token-ID"
            else if (token.startsWith("demo-token-")) {
                String idStr = token.substring("demo-token-".length());
                if (idStr.matches("\\d+")) {
                    return Long.parseLong(idStr);
                }
            }

            // Log that we couldn't extract the ID
            System.out.println("Could not extract teacher ID from token: " + token);

            // Important: Return null instead of defaulting to ID 1
            return null;
        } catch (Exception e) {
            System.out.println("Error parsing teacher ID from token: " + e.getMessage());
            return null;
        }
    }
}