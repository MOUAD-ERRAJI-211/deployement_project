package orthoproconnect.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import orthoproconnect.model.Document;
import orthoproconnect.model.Teacher;
import orthoproconnect.repository.DocumentRepository;
import orthoproconnect.repository.TeacherRepository;
import orthoproconnect.service.FileService;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private TeacherRepository teacherRepository;

    @Autowired
    private FileService fileService;

    // GET all documents
    @GetMapping
    public List<Document> getAllDocuments() {
        return documentRepository.findAll();
    }

    // GET document by ID
    @GetMapping("/{id}")
    public ResponseEntity<?> getDocumentById(@PathVariable Long id) {
        Optional<Document> document = documentRepository.findById(id);
        if (document.isPresent()) {
            return ResponseEntity.ok(document.get());
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Document non trouvÃ©");
        }
    }

    // GET documents by category
    @GetMapping("/category/{category}")
    public List<Document> getDocumentsByCategory(@PathVariable String category) {
        return documentRepository.findByCategory(category);
    }

    // GET documents by file type
    @GetMapping("/type/{fileType}")
    public List<Document> getDocumentsByFileType(@PathVariable String fileType) {
        return documentRepository.findByFileType(fileType);
    }

    // GET documents by search term
    @GetMapping("/search")
    public List<Document> searchDocuments(@RequestParam String term) {
        return documentRepository.findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(term, term);
    }

    // POST - upload document
    @PostMapping
    public ResponseEntity<?> uploadDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam("title") String title,
            @RequestParam("description") String description,
            @RequestParam("category") String category,
            @RequestParam("teacherId") Long teacherId) {

        try {
            // VÃ©rifier que l'enseignant existe
            Optional<Teacher> teacher = teacherRepository.findById(teacherId);
            if (!teacher.isPresent()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Enseignant non trouvÃ©");
            }

            // Enregistrer le fichier
            String filePath = fileService.saveFile(file, "docs");

            // Extraire le type de fichier
            String originalFilename = file.getOriginalFilename();
            String fileType = originalFilename.substring(originalFilename.lastIndexOf(".") + 1);

            // CrÃ©er l'entrÃ©e dans la base de donnÃ©es
            Document document = new Document();
            document.setTitle(title);
            document.setDescription(description);
            document.setFilePath(filePath);
            document.setFileType(fileType);
            document.setFileSize(String.format("%.2f KB", (double) file.getSize() / 1024));
            document.setCategory(category);
            document.setUploadedBy(teacher.get());

            Document savedDocument = documentRepository.save(document);
            return ResponseEntity.status(HttpStatus.CREATED).body(savedDocument);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur lors de l'upload du document: " + e.getMessage());
        }
    }

    // PUT - update document metadata
    @PutMapping("/{id}")
    public ResponseEntity<?> updateDocument(
            @PathVariable Long id,
            @RequestBody DocumentUpdateRequest request) {

        Optional<Document> existingDocument = documentRepository.findById(id);
        if (!existingDocument.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Document non trouvÃ©");
        }

        Document document = existingDocument.get();

        if (request.getTitle() != null) {
            document.setTitle(request.getTitle());
        }

        if (request.getDescription() != null) {
            document.setDescription(request.getDescription());
        }

        if (request.getCategory() != null) {
            document.setCategory(request.getCategory());
        }

        if (request.getThumbnailPath() != null) {
            document.setThumbnailPath(request.getThumbnailPath());
        }

        Document updatedDocument = documentRepository.save(document);
        return ResponseEntity.ok(updatedDocument);
    }

    // DELETE document
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteDocument(@PathVariable Long id) {
        Optional<Document> document = documentRepository.findById(id);
        if (!document.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Document non trouvÃ©");
        }

        try {
            // Supprimer le fichier du systÃ¨me de fichiers
            boolean fileDeleted = fileService.deleteFile(document.get().getFilePath());

            // Supprimer l'entrÃ©e de la base de donnÃ©es
            documentRepository.deleteById(id);

            Map<String, String> response = new HashMap<>();
            response.put("message", "Document supprimÃ© avec succÃ¨s" + (fileDeleted ? "" : " (fichier non trouvÃ©)"));
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur lors de la suppression du document: " + e.getMessage());
        }
    }

    // GET - download document
    @GetMapping("/download/{id}")
    public ResponseEntity<?> downloadDocument(@PathVariable Long id) {
        Optional<Document> document = documentRepository.findById(id);
        if (!document.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Document non trouvÃ©");
        }

        try {
            Path filePath = Paths.get(document.get().getFilePath());
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists()) {
                // IncrÃ©menter le compteur de tÃ©lÃ©chargements
                Document doc = document.get();
                doc.incrementDownloadCount();
                documentRepository.save(doc);

                String contentType = determineContentType(doc.getFileType());

                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(contentType))
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filePath.getFileName().toString() + "\"")
                        .body(resource);
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Fichier non trouvÃ©");
            }
        } catch (MalformedURLException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur lors du tÃ©lÃ©chargement: " + e.getMessage());
        }
    }

    // GET - list documents from filesystem
    @GetMapping("/list-files")
    public ResponseEntity<?> listDocumentFiles() {
        try {
            List<Map<String, Object>> files = fileService.listDocuments();
            return ResponseEntity.ok(files);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur lors de la lecture des fichiers: " + e.getMessage());
        }
    }

    // Helper method to determine content type
    private String determineContentType(String fileType) {
        switch (fileType.toLowerCase()) {
            case "pdf":
                return "application/pdf";
            case "doc":
                return "application/msword";
            case "docx":
                return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case "ppt":
                return "application/vnd.ms-powerpoint";
            case "pptx":
                return "application/vnd.openxmlformats-officedocument.presentationml.presentation";
            case "xls":
                return "application/vnd.ms-excel";
            case "xlsx":
                return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            default:
                return "application/octet-stream";
        }
    }

    // Import documents from filesystem
    @PostMapping("/import-files")
    public ResponseEntity<?> importDocumentsFromFiles(@RequestParam("teacherId") Long teacherId) {
        try {
            // VÃ©rifier que l'enseignant existe
            Optional<Teacher> teacher = teacherRepository.findById(teacherId);
            if (!teacher.isPresent()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Enseignant non trouvÃ©");
            }

            List<Map<String, Object>> files = fileService.listDocuments();
            int importedCount = 0;

            for (Map<String, Object> file : files) {
                // VÃ©rifier si le document existe dÃ©jÃ  en BDD
                String filePath = (String) file.get("path");
                boolean exists = documentRepository.findAll().stream()
                        .anyMatch(doc -> doc.getFilePath().equals(filePath));

                if (!exists) {
                    Document document = new Document();
                    document.setTitle((String) file.get("title"));
                    document.setDescription("Document importÃ©: " + file.get("title"));
                    document.setFilePath(filePath);
                    document.setFileType((String) file.get("type"));
                    document.setFileSize((String) file.get("size"));
                    document.setCategory(determineCategory((String) file.get("type")));
                    document.setUploadedBy(teacher.get());

                    documentRepository.save(document);
                    importedCount++;
                }
            }

            Map<String, Object> response = new HashMap<>();
            response.put("message", importedCount + " documents importÃ©s avec succÃ¨s");
            response.put("importedCount", importedCount);
            response.put("totalFiles", files.size());

            return ResponseEntity.ok(response);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur lors de l'importation des documents: " + e.getMessage());
        }
    }

    // DELETE file (for files without database entry)
    // DELETE file (for files without database entry)
    @DeleteMapping("/delete-file")
    public ResponseEntity<?> deleteFile(@RequestParam("path") String filePath) {
        try {
            boolean fileDeleted = fileService.deleteFile(filePath);

            if (fileDeleted) {
                // VÃ©rifier si le fichier a une entrÃ©e en base de donnÃ©es
                Optional<Document> document = documentRepository.findAll().stream()
                        .filter(doc -> doc.getFilePath().equals(filePath))
                        .findFirst();

                // Si oui, supprimer cette entrÃ©e Ã©galement
                if (document.isPresent()) {
                    documentRepository.deleteById(document.get().getId());
                }

                Map<String, String> response = new HashMap<>();
                response.put("message", "Fichier supprimÃ© avec succÃ¨s");
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Fichier non trouvÃ©");
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur lors de la suppression du fichier: " + e.getMessage());
        }
    }

    // Helper method to determine document category based on file type
    private String determineCategory(String fileType) {
        switch (fileType.toLowerCase()) {
            case "pdf":
                return "Documentation";
            case "doc":
            case "docx":
                return "Guide";
            case "ppt":
            case "pptx":
                return "PrÃ©sentation";
            case "xls":
            case "xlsx":
                return "DonnÃ©es";
            default:
                return "Divers";
        }
    }
}