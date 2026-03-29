package orthoproconnect.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import orthoproconnect.model.Piece;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PieceRepository extends JpaRepository<Piece, Long> {

    // Basic search methods
    Optional<Piece> findByReference(String reference);

    List<Piece> findByCategory(String category);

    List<Piece> findBySubcategory(String subcategory);

    List<Piece> findByLocation(String location);

    // Advanced search methods
    @Query("SELECT p FROM Piece p WHERE " +
            "LOWER(p.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(p.reference) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(p.category) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(p.subcategory) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    List<Piece> findBySearchTerm(@Param("searchTerm") String searchTerm);

    // Stock management queries
    @Query("SELECT p FROM Piece p WHERE p.currentStock < p.minThreshold")
    List<Piece> findLowStockPieces();

    @Query("SELECT p FROM Piece p WHERE p.currentStock <= 0")
    List<Piece> findOutOfStockPieces();

    @Query("SELECT p FROM Piece p WHERE p.currentStock BETWEEN :min AND :max")
    List<Piece> findByStockRange(@Param("min") Integer min, @Param("max") Integer max);

    // Most frequently used pieces
    @Query("SELECT t.piece, COUNT(t) as transactionCount FROM Transaction t " +
            "WHERE t.transactionType = 'CHECKOUT' " +
            "GROUP BY t.piece " +
            "ORDER BY transactionCount DESC")
    List<Object[]> findMostFrequentlyCheckedOutPieces();

    // Recent activity
    @Query("SELECT p FROM Piece p WHERE p.lastCheckout >= :since")
    List<Piece> findRecentlyCheckedOutPieces(@Param("since") LocalDateTime since);

    @Query("SELECT p FROM Piece p WHERE p.lastReturn >= :since")
    List<Piece> findRecentlyReturnedPieces(@Param("since") LocalDateTime since);

    // Category summary
    @Query("SELECT p.category, COUNT(p), SUM(p.currentStock), AVG(p.currentStock) " +
            "FROM Piece p " +
            "GROUP BY p.category " +
            "ORDER BY p.category")
    List<Object[]> getCategoryStatistics();

    // Pieces with transactions in date range
    @Query("SELECT DISTINCT t.piece FROM Transaction t " +
            "WHERE t.transactionDate BETWEEN :startDate AND :endDate")
    List<Piece> findPiecesWithTransactionsBetween(@Param("startDate") LocalDateTime startDate,
                                                  @Param("endDate") LocalDateTime endDate);

    // Pieces by type
    List<Piece> findByPieceType(Piece.PieceType pieceType);

    // Supplier queries
    List<Piece> findBySupplier(String supplier);

    @Query("SELECT DISTINCT p.supplier FROM Piece p WHERE p.supplier IS NOT NULL ORDER BY p.supplier")
    List<String> findAllSuppliers();

    // Location queries
    @Query("SELECT DISTINCT p.location FROM Piece p ORDER BY p.location")
    List<String> findAllLocations();

    @Query("SELECT DISTINCT p.category FROM Piece p ORDER BY p.category")
    List<String> findAllCategories();

    @Query("SELECT DISTINCT p.subcategory FROM Piece p WHERE p.category = :category ORDER BY p.subcategory")
    List<String> findSubcategoriesByCategory(@Param("category") String category);

    // Unit type queries
    @Query("SELECT DISTINCT p.unitType FROM Piece p WHERE p.unitType IS NOT NULL ORDER BY p.unitType")
    List<String> findAllUnitTypes();

    // Custom search with multiple filters
    @Query("SELECT p FROM Piece p WHERE " +
            "(:name IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :name, '%'))) AND " +
            "(:reference IS NULL OR LOWER(p.reference) LIKE LOWER(CONCAT('%', :reference, '%'))) AND " +
            "(:category IS NULL OR p.category = :category) AND " +
            "(:subcategory IS NULL OR p.subcategory = :subcategory) AND " +
            "(:location IS NULL OR p.location = :location) AND " +
            "(:minStock IS NULL OR p.currentStock >= :minStock) AND " +
            "(:maxStock IS NULL OR p.currentStock <= :maxStock) AND " +
            "(:pieceType IS NULL OR p.pieceType = :pieceType) AND " +
            "(:unitType IS NULL OR p.unitType = :unitType)")
    List<Piece> findByFilters(@Param("name") String name,
                              @Param("reference") String reference,
                              @Param("category") String category,
                              @Param("subcategory") String subcategory,
                              @Param("location") String location,
                              @Param("minStock") Integer minStock,
                              @Param("maxStock") Integer maxStock,
                              @Param("pieceType") Piece.PieceType pieceType,
                              @Param("unitType") String unitType);

    // Excel format specific queries
    @Query("SELECT p FROM Piece p ORDER BY p.id ASC")
    List<Piece> findAllOrderedById();

    // Value-based queries
    @Query("SELECT p FROM Piece p WHERE p.unitPrice IS NOT NULL ORDER BY p.unitPrice DESC")
    List<Piece> findPiecesOrderedByValueDescending();

    @Query("SELECT SUM(p.currentStock * COALESCE(p.unitPrice, 0)) FROM Piece p")
    Double getTotalInventoryValue();

    // Check existence
    boolean existsByReference(String reference);

    boolean existsByName(String name);

    // Get inventory by month
    @Query("SELECT p FROM Piece p WHERE FUNCTION('MONTH', p.updatedAt) = :month AND FUNCTION('YEAR', p.updatedAt) = :year")
    List<Piece> findByMonth(@Param("month") int month, @Param("year") int year);

    @Query("SELECT p FROM Piece p WHERE " +
            "(FUNCTION('MONTH', p.lastCheckout) = :month AND FUNCTION('YEAR', p.lastCheckout) = :year) OR " +
            "(FUNCTION('MONTH', p.lastReturn) = :month AND FUNCTION('YEAR', p.lastReturn) = :year) OR " +
            "(FUNCTION('MONTH', p.updatedAt) = :month AND FUNCTION('YEAR', p.updatedAt) = :year)")
    List<Piece> findByActivityMonth(@Param("month") int month, @Param("year") int year);

    Optional<Piece> findByQrCode(String qrCode);
    boolean existsByQrCode(String qrCode);
    // Get inventory with activity in month (checkout or return)
}