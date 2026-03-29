package orthoproconnect.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import orthoproconnect.model.Transaction;
import orthoproconnect.model.Student;
import orthoproconnect.model.Piece;
import orthoproconnect.model.Teacher;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    // Find by related entities
    List<Transaction> findByPiece(Piece piece);
    List<Transaction> findByStudent(Student student);
    // Find by authorized person
    @Query("SELECT t FROM Transaction t WHERE t.authorizedBy = :authorizedBy")
    List<Transaction> findByAuthorizedBy(@Param("authorizedBy") String authorizedBy);

    // Find by transaction type
    List<Transaction> findByTransactionType(Transaction.TransactionType transactionType);
    List<Transaction> findByTransactionTypeOrderByTransactionDateDesc(Transaction.TransactionType transactionType);

    // Find by status
    List<Transaction> findByStatus(Transaction.TransactionStatus status);

    // Find pending transactions (items not returned)
    @Query("SELECT t FROM Transaction t WHERE t.status = 'PENDING' AND t.transactionType = 'CHECKOUT'")
    List<Transaction> findPendingCheckouts();

    // Find overdue transactions
    @Query("SELECT t FROM Transaction t WHERE " +
            "t.status = 'PENDING' " +
            "AND t.expectedReturnDate < :currentDate " +
            "AND t.transactionType = 'CHECKOUT'")
    List<Transaction> findOverdueTransactions(@Param("currentDate") LocalDateTime currentDate);

    // Find transactions by piece and date range
    @Query("SELECT t FROM Transaction t WHERE " +
            "t.piece = :piece " +
            "AND t.transactionDate BETWEEN :startDate AND :endDate " +
            "ORDER BY t.transactionDate DESC")
    List<Transaction> findByPieceAndDateRange(@Param("piece") Piece piece,
                                              @Param("startDate") LocalDateTime startDate,
                                              @Param("endDate") LocalDateTime endDate);

    // Find transactions by student and date range
    @Query("SELECT t FROM Transaction t WHERE " +
            "t.student = :student " +
            "AND t.transactionDate BETWEEN :startDate AND :endDate " +
            "ORDER BY t.transactionDate DESC")
    List<Transaction> findByStudentAndDateRange(@Param("student") Student student,
                                                @Param("startDate") LocalDateTime startDate,
                                                @Param("endDate") LocalDateTime endDate);

    // Find active checkouts by student
    @Query("SELECT t FROM Transaction t WHERE " +
            "t.student = :student " +
            "AND t.status = 'PENDING' " +
            "AND t.transactionType = 'CHECKOUT'")
    List<Transaction> findActiveCheckoutsByStudent(@Param("student") Student student);

    // Find recent transactions
    @Query("SELECT t FROM Transaction t WHERE t.transactionDate >= :since ORDER BY t.transactionDate DESC")
    List<Transaction> findRecentTransactions(@Param("since") LocalDateTime since);

    // Find transactions in date range
    @Query("SELECT t FROM Transaction t WHERE " +
            "t.transactionDate BETWEEN :startDate AND :endDate " +
            "ORDER BY t.transactionDate DESC")
    List<Transaction> findByDateRange(@Param("startDate") LocalDateTime startDate,
                                      @Param("endDate") LocalDateTime endDate);

    // Daily transaction statistics
    @Query("SELECT DATE(t.transactionDate), t.transactionType, COUNT(t), SUM(t.quantity) " +
            "FROM Transaction t " +
            "WHERE t.transactionDate BETWEEN :startDate AND :endDate " +
            "GROUP BY DATE(t.transactionDate), t.transactionType " +
            "ORDER BY DATE(t.transactionDate) DESC")
    List<Object[]> getDailyTransactionStatistics(@Param("startDate") LocalDateTime startDate,
                                                 @Param("endDate") LocalDateTime endDate);

    // Most active pieces (by transaction count)
    @Query("SELECT t.piece, COUNT(t) as transactionCount " +
            "FROM Transaction t " +
            "WHERE t.transactionDate BETWEEN :startDate AND :endDate " +
            "GROUP BY t.piece " +
            "ORDER BY transactionCount DESC")
    List<Object[]> getMostActiveItems(@Param("startDate") LocalDateTime startDate,
                                      @Param("endDate") LocalDateTime endDate);

    // Most active students (by transaction count)
    @Query("SELECT t.student, COUNT(t) as transactionCount " +
            "FROM Transaction t " +
            "WHERE t.transactionDate BETWEEN :startDate AND :endDate " +
            "GROUP BY t.student " +
            "ORDER BY transactionCount DESC")
    List<Object[]> getMostActiveStudents(@Param("startDate") LocalDateTime startDate,
                                         @Param("endDate") LocalDateTime endDate);

    // Transaction type distribution
    @Query("SELECT t.transactionType, COUNT(t), SUM(t.quantity) " +
            "FROM Transaction t " +
            "WHERE t.transactionDate BETWEEN :startDate AND :endDate " +
            "GROUP BY t.transactionType")
    List<Object[]> getTransactionTypeDistribution(@Param("startDate") LocalDateTime startDate,
                                                  @Param("endDate") LocalDateTime endDate);

    // Find transactions by piece and student
    @Query("SELECT t FROM Transaction t WHERE " +
            "t.piece = :piece AND t.student = :student " +
            "ORDER BY t.transactionDate DESC")
    List<Transaction> findByPieceAndStudent(@Param("piece") Piece piece, @Param("student") Student student);

    // Find last checkout of a piece by a student (for return validation)
    @Query("SELECT t FROM Transaction t WHERE " +
            "t.piece = :piece AND t.student = :student " +
            "AND t.transactionType = 'CHECKOUT' AND t.status = 'PENDING' " +
            "ORDER BY t.transactionDate DESC")
    List<Transaction> findPendingCheckoutByPieceAndStudent(@Param("piece") Piece piece, @Param("student") Student student);

    // Count checkouts and returns for a piece
    @Query("SELECT " +
            "COUNT(CASE WHEN t.transactionType = 'CHECKOUT' THEN 1 END) as checkouts, " +
            "COUNT(CASE WHEN t.transactionType = 'RETURN' THEN 1 END) as returns " +
            "FROM Transaction t WHERE t.piece = :piece")
    Object[] getCheckoutReturnCountByPiece(@Param("piece") Piece piece);

    // Find transactions by multiple criteria
    @Query("SELECT t FROM Transaction t WHERE " +
            "(:pieceId IS NULL OR t.piece.id = :pieceId) AND " +
            "(:studentId IS NULL OR t.student.id = :studentId) AND " +
            "(:authorizedBy IS NULL OR t.authorizedBy = :authorizedBy) AND " +
            "(:transactionType IS NULL OR t.transactionType = :transactionType) AND " +
            "(:status IS NULL OR t.status = :status) AND " +
            "(:startDate IS NULL OR t.transactionDate >= :startDate) AND " +
            "(:endDate IS NULL OR t.transactionDate <= :endDate) " +
            "ORDER BY t.transactionDate DESC")
    List<Transaction> findByMultipleCriteria(@Param("pieceId") Long pieceId,
                                             @Param("studentId") Long studentId,
                                             @Param("authorizedBy") String authorizedBy,
                                             @Param("transactionType") Transaction.TransactionType transactionType,
                                             @Param("status") Transaction.TransactionStatus status,
                                             @Param("startDate") LocalDateTime startDate,
                                             @Param("endDate") LocalDateTime endDate);

    // Get hourly transaction distribution
    @Query("SELECT HOUR(t.transactionDate), COUNT(t) " +
            "FROM Transaction t " +
            "WHERE t.transactionDate BETWEEN :startDate AND :endDate " +
            "GROUP BY HOUR(t.transactionDate) " +
            "ORDER BY HOUR(t.transactionDate)")
    List<Object[]> getHourlyTransactionDistribution(@Param("startDate") LocalDateTime startDate,
                                                    @Param("endDate") LocalDateTime endDate);

    // Get category usage statistics
    @Query("SELECT p.category, COUNT(t), SUM(t.quantity) " +
            "FROM Transaction t JOIN t.piece p " +
            "WHERE t.transactionType = 'CHECKOUT' " +
            "AND t.transactionDate BETWEEN :startDate AND :endDate " +
            "GROUP BY p.category " +
            "ORDER BY COUNT(t) DESC")
    List<Object[]> getCategoryUsageStatistics(@Param("startDate") LocalDateTime startDate,
                                              @Param("endDate") LocalDateTime endDate);
}