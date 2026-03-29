package orthoproconnect.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "pieces")
public class Piece {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(name = "reference", nullable = true)
    private String reference;

    @Column(nullable = false)
    private String category;

    @Column(nullable = false)
    private String subcategory;

    @Column(name = "current_stock", nullable = false)
    private Integer currentStock;

    @Column(name = "initial_stock", nullable = false)
    private Integer initialStock;

    @Column(name = "min_threshold")
    private Integer minThreshold;

    @Column(name = "max_threshold")
    private Integer maxThreshold;

    @Column(name = "unit_price")
    private Double unitPrice;

    @Enumerated(EnumType.STRING)
    @Column(name = "piece_type", nullable = false)
    private PieceType pieceType;

    @Column(nullable = false)
    private String location;

    @Column(name = "shelf_position")
    private String shelfPosition;

    @Column(name = "supplier")
    private String supplier;

    @Column(name = "supplier_reference")
    private String supplierReference;

    @Column(name = "unit_type")
    private String unitType;

    @Column(name = "quantity_per_unit")
    private Integer quantityPerUnit;

    @Column(name = "entries")
    private Integer entries = 0;

    @Column(name = "exits")
    private Integer exits = 0;




    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "qr_code", unique = true)
    private String qrCode;

    // Add getter and setter methods in the getters/setters section
    public String getQrCode() { return qrCode; }
    public void setQrCode(String qrCode) { this.qrCode = qrCode; }

    @Column(name = "last_checkout")
    private LocalDateTime lastCheckout;

    @Column(name = "last_return")
    private LocalDateTime lastReturn;

    // Enum for piece types
    public enum PieceType {
        CONSUMABLE,     // Items that are consumed and don't need to be returned
        NON_CONSUMABLE, // Items that must be returned after use
        TOOL,           // Special tools that can be borrowed
        SPARE_PART      // Replacement parts
    }

    // Constructors
    public Piece() {
        this.entries = 0;
        this.exits = 0;
    }

    public Piece(String name, String reference, String category, String subcategory,
                 Integer initialStock, PieceType pieceType, String location) {
        this.name = name;
        this.reference = reference;
        this.category = category;
        this.subcategory = subcategory;
        this.initialStock = initialStock;
        this.currentStock = initialStock;
        this.pieceType = pieceType;
        this.location = location;
        this.entries = 0;
        this.exits = 0;

        // Set default unit type based on piece type
        this.setDefaultUnitType();
    }



    private void setDefaultUnitType() {
        if (this.pieceType == PieceType.CONSUMABLE) {
            this.unitType = "UnitÃ©";
            this.quantityPerUnit = 1;
        } else if (this.pieceType == PieceType.SPARE_PART) {
            this.unitType = "PiÃ¨ce";
            this.quantityPerUnit = 1;
        } else if (this.name != null && this.name.toLowerCase().contains("paire")) {
            this.unitType = "Paire";
            this.quantityPerUnit = 2;
        } else {
            this.unitType = "UnitÃ©";
            this.quantityPerUnit = 1;
        }
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();

        if (unitType == null) {
            setDefaultUnitType();
        }

        if (quantityPerUnit == null) {
            if ("Paire".equals(unitType)) {
                quantityPerUnit = 2;
            } else {
                quantityPerUnit = 1;
            }
        }

        if (entries == null) entries = 0;
        if (exits == null) exits = 0;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getReference() { return reference; }
    public void setReference(String reference) { this.reference = reference; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getSubcategory() { return subcategory; }
    public void setSubcategory(String subcategory) { this.subcategory = subcategory; }

    public Integer getCurrentStock() { return currentStock; }
    public void setCurrentStock(Integer currentStock) { this.currentStock = currentStock; }

    public Integer getInitialStock() { return initialStock; }
    public void setInitialStock(Integer initialStock) { this.initialStock = initialStock; }

    public Integer getMinThreshold() { return minThreshold; }
    public void setMinThreshold(Integer minThreshold) { this.minThreshold = minThreshold; }

    public Integer getMaxThreshold() { return maxThreshold; }
    public void setMaxThreshold(Integer maxThreshold) { this.maxThreshold = maxThreshold; }

    public Double getUnitPrice() { return unitPrice; }
    public void setUnitPrice(Double unitPrice) { this.unitPrice = unitPrice; }

    public PieceType getPieceType() { return pieceType; }
    public void setPieceType(PieceType pieceType) {
        this.pieceType = pieceType;
        setDefaultUnitType();
    }


    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getShelfPosition() { return shelfPosition; }
    public void setShelfPosition(String shelfPosition) { this.shelfPosition = shelfPosition; }

    public String getSupplier() { return supplier; }
    public void setSupplier(String supplier) { this.supplier = supplier; }

    public String getSupplierReference() { return supplierReference; }
    public void setSupplierReference(String supplierReference) { this.supplierReference = supplierReference; }

    public String getUnitType() { return unitType; }
    public void setUnitType(String unitType) { this.unitType = unitType; }

    public Integer getQuantityPerUnit() { return quantityPerUnit; }
    public void setQuantityPerUnit(Integer quantityPerUnit) { this.quantityPerUnit = quantityPerUnit; }

    public Integer getEntries() { return entries; }
    public void setEntries(Integer entries) { this.entries = entries; }

    public Integer getExits() { return exits; }
    public void setExits(Integer exits) { this.exits = exits; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public LocalDateTime getLastCheckout() { return lastCheckout; }
    public void setLastCheckout(LocalDateTime lastCheckout) { this.lastCheckout = lastCheckout; }

    public LocalDateTime getLastReturn() { return lastReturn; }
    public void setLastReturn(LocalDateTime lastReturn) { this.lastReturn = lastReturn; }

    // Helper methods
    public boolean isLowStock() {
        return minThreshold != null && currentStock <= minThreshold;
    }

    public boolean isOutOfStock() {
        return currentStock <= 0;
    }

    public boolean canCheckout(int quantity) {
        if (pieceType == PieceType.CONSUMABLE) {
            return currentStock >= quantity;
        } else {
            return currentStock > 0;
        }
    }

    public void updateStock(int change) {
        this.currentStock += change;

        // Initialize exits if null to prevent NullPointerException
        if (this.exits == null) {
            this.exits = 0;
        }

        // Initialize entries if null to prevent potential similar issues
        if (this.entries == null) {
            this.entries = 0;
        }

        if (change < 0) {
            this.lastCheckout = LocalDateTime.now();
            this.exits += Math.abs(change);
        } else {
            this.lastReturn = LocalDateTime.now();
            this.entries += change;
        }
    }

    // Get stock final (current stock)
    public Integer getStockFinal() {
        return this.currentStock;
    }
}