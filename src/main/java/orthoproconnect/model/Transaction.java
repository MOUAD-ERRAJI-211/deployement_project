package orthoproconnect.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "transactions")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "piece_id", nullable = false)
    private Piece piece;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Column(nullable = false)
    private Integer quantity;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false)
    private TransactionType transactionType;

    @Column(name = "transaction_date", nullable = false)
    private LocalDateTime transactionDate;

    @Column(name = "expected_return_date")
    private LocalDateTime expectedReturnDate;

    @Column(name = "actual_return_date")
    private LocalDateTime actualReturnDate;

    @Column(name = "additional_students", columnDefinition = "TEXT")
    private String additionalStudents; // JSON array of student codes

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "authorized_by")
    private String authorizedBy; // Who authorized the transaction (admin/teacher name)

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private TransactionStatus status;

    @Column(name = "returned_quantity")
    private Integer returnedQuantity = 0;

    // Add these methods to your Transaction class

    // Add this field near the top of your class, after the other field declarations
    @Transient
    private StudentDetails studentDetails;

    // Add this inner class inside your Transaction class
    public static class StudentDetails {
        private Long id;
        private String fullName;
        private String studentCode;

        public StudentDetails(Student student) {
            if (student != null) {
                this.id = student.getId();
                this.fullName = student.getFullName();
                this.studentCode = student.getStudentCode();
            }
        }

        // Getters
        public Long getId() { return id; }
        public String getFullName() { return fullName; }
        public String getStudentCode() { return studentCode; }
    }

    // Add this method to your Transaction class
    @PostLoad
    public void populateTransientFields() {
        if (this.student != null) {
            this.studentDetails = new StudentDetails(this.student);
        }
    }

    // Add this getter method
    public StudentDetails getStudentDetails() {
        return studentDetails;
    }

    // Transaction types
    public enum TransactionType {
        CHECKOUT,
        RETURN,
        ADJUSTMENT,      // Manual stock adjustment by admin
        CONSUMPTION,     // Consumable item usage
        DAMAGED,         // Item marked as damaged
        LOST            // Item marked as lost
    }

    // Transaction status
    public enum TransactionStatus {
        PENDING,       // Item checked out, awaiting return
        COMPLETED,     // Item returned successfully
        OVERDUE,       // Return date passed
        PARTIAL,       // Partially returned (for multi-quantity items)
        CANCELLED      // Transaction cancelled
    }

    // Constructors
    public Transaction() {
    }

    public Transaction(Piece piece, Student student, Integer quantity,
                       TransactionType transactionType) {
        this.piece = piece;
        this.student = student;
        this.quantity = quantity;
        this.transactionType = transactionType;
        this.transactionDate = LocalDateTime.now();
        this.status = (transactionType == TransactionType.CHECKOUT)
                ? TransactionStatus.PENDING
                : TransactionStatus.COMPLETED;
    }

    public Transaction(Piece piece, Student student, Integer quantity,
                       TransactionType transactionType, String authorizedBy) {
        this(piece, student, quantity, transactionType);
        this.authorizedBy = authorizedBy;
    }

    @PrePersist
    protected void onCreate() {
        if (transactionDate == null) {
            transactionDate = LocalDateTime.now();
        }
        if (status == null) {
            status = (transactionType == TransactionType.CHECKOUT)
                    ? TransactionStatus.PENDING
                    : TransactionStatus.COMPLETED;
        }
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Piece getPiece() { return piece; }
    public void setPiece(Piece piece) { this.piece = piece; }

    public Student getStudent() { return student; }
    public void setStudent(Student student) { this.student = student; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public TransactionType getTransactionType() { return transactionType; }
    public void setTransactionType(TransactionType transactionType) { this.transactionType = transactionType; }

    public LocalDateTime getTransactionDate() { return transactionDate; }
    public void setTransactionDate(LocalDateTime transactionDate) { this.transactionDate = transactionDate; }

    public LocalDateTime getExpectedReturnDate() { return expectedReturnDate; }
    public void setExpectedReturnDate(LocalDateTime expectedReturnDate) { this.expectedReturnDate = expectedReturnDate; }

    public LocalDateTime getActualReturnDate() { return actualReturnDate; }
    public void setActualReturnDate(LocalDateTime actualReturnDate) { this.actualReturnDate = actualReturnDate; }

    public String getAdditionalStudents() { return additionalStudents; }
    public void setAdditionalStudents(String additionalStudents) { this.additionalStudents = additionalStudents; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getAuthorizedBy() { return authorizedBy; }
    public void setAuthorizedBy(String authorizedBy) { this.authorizedBy = authorizedBy; }

    public TransactionStatus getStatus() { return status; }
    public void setStatus(TransactionStatus status) { this.status = status; }

    public Integer getReturnedQuantity() { return returnedQuantity; }
    public void setReturnedQuantity(Integer returnedQuantity) { this.returnedQuantity = returnedQuantity; }

    // Helper methods
    public boolean isOverdue() {
        return expectedReturnDate != null
                && expectedReturnDate.isBefore(LocalDateTime.now())
                && status == TransactionStatus.PENDING;
    }

    public boolean isCompleted() {
        return status == TransactionStatus.COMPLETED;
    }

    public boolean canBeReturned() {
        return status == TransactionStatus.PENDING
                && transactionType == TransactionType.CHECKOUT;
    }

    public void markAsReturned() {
        this.actualReturnDate = LocalDateTime.now();
        this.status = TransactionStatus.COMPLETED;
        this.returnedQuantity = this.quantity;
    }

    public void markAsPartiallyReturned(int returnedQty) {
        this.returnedQuantity = returnedQty;
        if (returnedQty >= quantity) {
            this.actualReturnDate = LocalDateTime.now();
            this.status = TransactionStatus.COMPLETED;
        } else {
            this.status = TransactionStatus.PARTIAL;
        }
    }

    // Calculate remaining quantity to be returned
    public int getRemainingQuantity() {
        return quantity - returnedQuantity;
    }

    @Override
    public String toString() {
        return "Transaction{" +
                "id=" + id +
                ", piece=" + (piece != null ? piece.getName() : "null") +
                ", student=" + (student != null ? student.getFullName() : "null") +
                ", quantity=" + quantity +
                ", transactionType=" + transactionType +
                ", status=" + status +
                ", transactionDate=" + transactionDate +
                '}';
    }
}