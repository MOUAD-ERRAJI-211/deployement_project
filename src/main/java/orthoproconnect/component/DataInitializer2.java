package orthoproconnect.component;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import orthoproconnect.model.Piece;
import orthoproconnect.model.Student;
import orthoproconnect.repository.PieceRepository;
import orthoproconnect.repository.StudentRepository;

import java.util.Arrays;
import java.util.List;

@Component
public class DataInitializer2 implements CommandLineRunner {

    @Autowired
    private PieceRepository pieceRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Override
    public void run(String... args) throws Exception {
        // Only initialize if database is empty
        if (pieceRepository.count() == 0) {
            initializePieces();
        }

        if (studentRepository.count() == 0) {
            initializeStudents();
        }
    }

    private void initializePieces() {
        List<Piece> pieces = Arrays.asList();

        pieceRepository.saveAll(pieces);
        System.out.println("Initialisé " + pieces.size() + " pièces");
    }

    private Piece createPiece(String name, String reference, String category, String subcategory,
                              int currentStock, int maxStock, Piece.PieceType type,
                              String location, String shelfPosition, double unitPrice) {
        Piece piece = new Piece();
        piece.setName(name);
        piece.setReference(reference);
        piece.setCategory(category);
        piece.setSubcategory(subcategory);
        piece.setCurrentStock(currentStock);
        piece.setInitialStock(currentStock);
        piece.setMaxThreshold(maxStock);
        piece.setMinThreshold((int) (currentStock * 0.2)); // 20% of current stock as minimum
        piece.setPieceType(type);
        piece.setLocation(location);
        piece.setShelfPosition(shelfPosition);
        piece.setUnitPrice(unitPrice);
        piece.setSupplier("Fournisseur " + category);
        piece.setDescription("Description pour " + name);
        return piece;
    }

    private void initializeStudents() {
        List<Student> students = Arrays.asList(
                createStudent("Ahmed Ben Ali", "STU-2024-001", "ahmed.benali@example.com", "3ème année A"),
                createStudent("Fatima Zahra", "STU-2024-002", "fatima.zahra@example.com", "3ème année A"),
                createStudent("Youssef El Amrani", "STU-2024-003", "youssef.elamrani@example.com", "3ème année B"),
                createStudent("Aicha Benjelloun", "STU-2024-004", "aicha.benjelloun@example.com", "2ème année A"),
                createStudent("Mohammed Tazi", "STU-2024-005", "mohammed.tazi@example.com", "2ème année B"),
                createStudent("Salma Qadiri", "STU-2024-006", "salma.qadiri@example.com", "1ère année A"),
                createStudent("Omar Fassi", "STU-2024-007", "omar.fassi@example.com", "1ère année B"),
                createStudent("Khadija Alaoui", "STU-2024-008", "khadija.alaoui@example.com", "3ème année A"),
                createStudent("Hassan Berrada", "STU-2024-009", "hassan.berrada@example.com", "2ème année A"),
                createStudent("Nadia Chraibi", "STU-2024-010", "nadia.chraibi@example.com", "1ère année A"),
                createStudent("Rachid Bennani", "STU-2024-011", "rachid.bennani@example.com", "3ème année B"),
                createStudent("Laila Slaoui", "STU-2024-012", "laila.slaoui@example.com", "2ème année B"),
                createStudent("Abdelaziz Kettani", "STU-2024-013", "abdelaziz.kettani@example.com", "1ère année B"),
                createStudent("Hajar Idrissi", "STU-2024-014", "hajar.idrissi@example.com", "3ème année A"),
                createStudent("Karim Ouazzani", "STU-2024-015", "karim.ouazzani@example.com", "2ème année A")
        );

        studentRepository.saveAll(students);
        System.out.println("Initialisé " + students.size() + " étudiants");
    }

    private Student createStudent(String fullName, String studentCode, String email, String classGroup) {
        Student student = new Student();
        student.setFullName(fullName);
        student.setStudentCode(studentCode);
        student.setEmail(email);
        student.setClassGroup(classGroup);
        student.setAcademicYear("2024-2025");
        student.setStatus(Student.StudentStatus.ACTIVE);
        return student;
    }
}