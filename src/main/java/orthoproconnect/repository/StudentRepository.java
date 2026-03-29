package orthoproconnect.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import orthoproconnect.model.Student;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface StudentRepository extends JpaRepository<Student, Long> {

    // Basic find methods
    Optional<Student> findByStudentCode(String studentCode);
    Optional<Student> findByQrCode(String qrCode);
    Optional<Student> findByQrCodeData(String qrCodeData);
    Optional<Student> findByEmail(String email);

    // Find by status
    List<Student> findByStatus(Student.StudentStatus status);
    List<Student> findByStatusOrderByFullNameAsc(Student.StudentStatus status);

    // Find active students
    @Query("SELECT s FROM Student s WHERE s.status = 'ACTIVE'")
    List<Student> findActiveStudents();

    // Search by name or code
    @Query("SELECT s FROM Student s WHERE " +
            "LOWER(s.fullName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(s.studentCode) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    List<Student> findByNameOrCode(@Param("searchTerm") String searchTerm);

    

    // Find by class group
    List<Student> findByClassGroup(String classGroup);
    List<Student> findByClassGroupOrderByFullNameAsc(String classGroup);

    // Find by academic year
    List<Student> findByAcademicYear(String academicYear);

    // Recent activity
    @Query("SELECT s FROM Student s WHERE s.lastActivity >= :since ORDER BY s.lastActivity DESC")
    List<Student> findByRecentActivity(@Param("since") LocalDateTime since);

    // Students with transactions
    @Query("SELECT DISTINCT t.student FROM Transaction t " +
            "WHERE t.transactionDate BETWEEN :startDate AND :endDate " +
            "ORDER BY t.student.fullName")
    List<Student> findStudentsWithTransactionsBetween(@Param("startDate") LocalDateTime startDate,
                                                      @Param("endDate") LocalDateTime endDate);

    // Students with pending returns
    @Query("SELECT DISTINCT t.student FROM Transaction t " +
            "WHERE t.status = 'PENDING' AND t.transactionType = 'CHECKOUT'")
    List<Student> findStudentsWithPendingReturns();

    // Students with overdue items
    @Query("SELECT DISTINCT t.student FROM Transaction t " +
            "WHERE t.status = 'PENDING' " +
            "AND t.expectedReturnDate < :currentDate " +
            "AND t.transactionType = 'CHECKOUT'")
    List<Student> findStudentsWithOverdueItems(@Param("currentDate") LocalDateTime currentDate);

    // Count methods
    long countByStatus(Student.StudentStatus status);
    long countByClassGroup(String classGroup);
    long countByAcademicYear(String academicYear);

    // Validation methods
    boolean existsByStudentCode(String studentCode);
    boolean existsByQrCode(String qrCode);
    boolean existsByQrCodeData(String qrCodeData);
    boolean existsByEmail(String email);

    // Get all class groups
    @Query("SELECT DISTINCT s.classGroup FROM Student s WHERE s.classGroup IS NOT NULL ORDER BY s.classGroup")
    List<String> findAllClassGroups();

    // Get all academic years
    @Query("SELECT DISTINCT s.academicYear FROM Student s WHERE s.academicYear IS NOT NULL ORDER BY s.academicYear DESC")
    List<String> findAllAcademicYears();

    // Student activity statistics
    @Query("SELECT s, COUNT(t) as transactionCount FROM Student s " +
            "LEFT JOIN Transaction t ON s = t.student " +
            "GROUP BY s " +
            "ORDER BY transactionCount DESC")
    List<Object[]> findStudentActivityStatistics();

    // Find students by multiple criteria
    @Query("SELECT s FROM Student s WHERE " +
            "(:name IS NULL OR LOWER(s.fullName) LIKE LOWER(CONCAT('%', :name, '%'))) AND " +
            "(:studentCode IS NULL OR LOWER(s.studentCode) LIKE LOWER(CONCAT('%', :studentCode, '%'))) AND " +
            "(:classGroup IS NULL OR s.classGroup = :classGroup) AND " +
            "(:academicYear IS NULL OR s.academicYear = :academicYear) AND " +
            "(:status IS NULL OR s.status = :status)")
    List<Student> findByMultipleCriteria(@Param("name") String name,
                                         @Param("studentCode") String studentCode,
                                         @Param("classGroup") String classGroup,
                                         @Param("academicYear") String academicYear,
                                         @Param("status") Student.StudentStatus status);
}