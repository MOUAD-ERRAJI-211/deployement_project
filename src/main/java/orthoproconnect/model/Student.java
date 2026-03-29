package orthoproconnect.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "students")
public class Student {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(name = "student_code", nullable = false, unique = true)
    private String studentCode;

    @Column(name = "qr_code", nullable = false, unique = true)
    private String qrCode;

    @Column(name = "qr_code_path")
    private String qrCodePath; // Stores path to QR code image

    @Column(name = "qr_code_data", unique = true)
    private String qrCodeData; // Stores unique QR identifier

    @Column(name = "email")
    private String email;

    @Column(name = "phone_number")
    private String phoneNumber;

    @Column(name = "class_group")
    private String classGroup;

    @Column(name = "academic_year")
    private String academicYear;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private StudentStatus status = StudentStatus.ACTIVE;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "last_activity")
    private LocalDateTime lastActivity;

    // Student status enum
    public enum StudentStatus {
        ACTIVE,
        INACTIVE,
        GRADUATED,
        SUSPENDED
    }

    // Constructors
    public Student() {
        this.qrCode = generateQRCode();
    }

    public Student(String fullName, String studentCode) {
        this();
        this.fullName = fullName;
        this.studentCode = studentCode;
    }

    public Student(String fullName, String studentCode, String email, String classGroup) {
        this(fullName, studentCode);
        this.email = email;
        this.classGroup = classGroup;
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (qrCode == null) {
            qrCode = generateQRCode();
        }
        if (qrCodeData == null) {
            // Generate unique QR data when creating the student
            qrCodeData = "STUDENT-" + UUID.randomUUID().toString();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Generate unique QR code
    private String generateQRCode() {
        return "QR-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getStudentCode() { return studentCode; }
    public void setStudentCode(String studentCode) { this.studentCode = studentCode; }

    public String getQrCode() { return qrCode; }
    public void setQrCode(String qrCode) { this.qrCode = qrCode; }

    public String getQrCodePath() { return qrCodePath; }
    public void setQrCodePath(String qrCodePath) { this.qrCodePath = qrCodePath; }

    public String getQrCodeData() { return qrCodeData; }
    public void setQrCodeData(String qrCodeData) { this.qrCodeData = qrCodeData; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public String getClassGroup() { return classGroup; }
    public void setClassGroup(String classGroup) { this.classGroup = classGroup; }

    public String getAcademicYear() { return academicYear; }
    public void setAcademicYear(String academicYear) { this.academicYear = academicYear; }

    public StudentStatus getStatus() { return status; }
    public void setStatus(StudentStatus status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public LocalDateTime getLastActivity() { return lastActivity; }
    public void setLastActivity(LocalDateTime lastActivity) { this.lastActivity = lastActivity; }

    // Helper methods
    public boolean isActive() {
        return status == StudentStatus.ACTIVE;
    }

    public void updateActivity() {
        this.lastActivity = LocalDateTime.now();
    }

    @Override
    public String toString() {
        return "Student{" +
                "id=" + id +
                ", fullName='" + fullName + '\'' +
                ", studentCode='" + studentCode + '\'' +
                ", qrCode='" + qrCode + '\'' +
                ", status=" + status +
                '}';
    }
}