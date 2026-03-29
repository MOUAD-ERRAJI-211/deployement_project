package orthoproconnect.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import orthoproconnect.model.Document;
import orthoproconnect.model.Teacher;
import orthoproconnect.model.Test;
import orthoproconnect.repository.DocumentRepository;
import orthoproconnect.repository.TeacherRepository;
import orthoproconnect.repository.TestRepository;
import orthoproconnect.service.FileService;

import java.util.List;
import java.util.Map;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private TeacherRepository teacherRepository;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private TestRepository testRepository;

    @Autowired
    private FileService fileService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        // Créer un enseignant par défaut s'il n'existe pas
        if (teacherRepository.count() == 0) {
            Teacher admin = new Teacher();
            admin.setFirstname("Admin");
            admin.setLastname("System");
            admin.setEmail("admin@orthoproconnect.fr");
            admin.setPasswordHash(passwordEncoder.encode("admin123"));
            admin.setInstitution("OrthoPro+ Academy");
            admin.setApproved(true);
            teacherRepository.save(admin);

            System.out.println("Enseignant par défaut créé !");

            // Importer les documents existants
            importExistingFiles(admin);
        }
    }

    private void importExistingFiles(Teacher teacher) {
        try {
            // Importer les documents
            List<Map<String, Object>> docFiles = fileService.listDocuments();
            System.out.println("Trouvé " + docFiles.size() + " documents à importer");

            for (Map<String, Object> file : docFiles) {
                String filePath = (String) file.get("path");
                Document document = new Document();
                document.setTitle((String) file.get("title"));
                document.setDescription("Document importé: " + file.get("title"));
                document.setFilePath(filePath);
                document.setFileType((String) file.get("type"));
                document.setFileSize((String) file.get("size"));
                document.setCategory(determineCategory((String) file.get("type")));
                document.setUploadedBy(teacher);

                documentRepository.save(document);
                System.out.println("Document importé: " + file.get("title"));
            }

            // Importer les tests
            List<Map<String, Object>> testFiles = fileService.listTests();
            System.out.println("Trouvé " + testFiles.size() + " tests à importer");

            for (Map<String, Object> file : testFiles) {
                String filePath = (String) file.get("path");
                Test test = new Test();
                test.setTitle((String) file.get("title"));
                test.setDescription("Test importé: " + file.get("title"));
                test.setFilePath(filePath);
                test.setLevel((String) file.get("level"));
                test.setDuration((String) file.get("duration"));
                test.setQuestions((Integer) file.get("questions"));
                test.setCreatedBy(teacher);

                testRepository.save(test);
                System.out.println("Test importé: " + file.get("title"));
            }
        } catch (Exception e) {
            System.err.println("Erreur lors de l'importation des fichiers: " + e.getMessage());
            e.printStackTrace();
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
                return "Présentation";
            case "xls":
            case "xlsx":
                return "Données";
            default:
                return "Divers";
        }
    }
}
