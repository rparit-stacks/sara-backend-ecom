package com.sara.ecom.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "product_pricing_slabs")
public class ProductPricingSlab {
    
    public enum DiscountType {
        FIXED_AMOUNT,  // Fixed discount per meter (e.g., ₹10 less per meter)
        PERCENTAGE     // Percentage discount (e.g., 10% off)
    }
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;
    
    @Column(name = "min_quantity", nullable = false)
    private Integer minQuantity; // Minimum quantity (meters) for this slab
    
    @Column(name = "max_quantity")
    private Integer maxQuantity; // Maximum quantity (meters) for this slab (null means no upper limit)
    
    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type")
    private DiscountType discountType; // Type of discount: FIXED_AMOUNT or PERCENTAGE (nullable for migration)
    
    @Column(name = "discount_value", precision = 10, scale = 2)
    private BigDecimal discountValue; // Discount amount (₹X for FIXED_AMOUNT, X% for PERCENTAGE) (nullable for migration)
    
    @Column(name = "display_order")
    private Integer displayOrder; // Order for displaying slabs
    
    // Legacy field - kept for backward compatibility, will be deprecated
    @Column(name = "price_per_meter", precision = 10, scale = 2)
    private BigDecimal pricePerMeter;
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Product getProduct() {
        return product;
    }
    
    public void setProduct(Product product) {
        this.product = product;
    }
    
    public Integer getMinQuantity() {
        return minQuantity;
    }
    
    public void setMinQuantity(Integer minQuantity) {
        this.minQuantity = minQuantity;
    }
    
    public Integer getMaxQuantity() {
        return maxQuantity;
    }
    
    public void setMaxQuantity(Integer maxQuantity) {
        this.maxQuantity = maxQuantity;
    }
    
    public DiscountType getDiscountType() {
        return discountType;
    }
    
    public void setDiscountType(DiscountType discountType) {
        this.discountType = discountType;
    }
    
    public BigDecimal getDiscountValue() {
        return discountValue;
    }
    
    public void setDiscountValue(BigDecimal discountValue) {
        this.discountValue = discountValue;
    }
    
    // Legacy getter/setter - kept for backward compatibility
    public BigDecimal getPricePerMeter() {
        return pricePerMeter;
    }
    
    public void setPricePerMeter(BigDecimal pricePerMeter) {
        this.pricePerMeter = pricePerMeter;
    }
    
    public Integer getDisplayOrder() {
        return displayOrder;
    }
    
    public void setDisplayOrder(Integer displayOrder) {
        this.displayOrder = displayOrder;
    }
}
