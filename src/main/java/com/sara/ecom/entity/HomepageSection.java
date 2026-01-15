package com.sara.ecom.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "homepage_sections")
public class HomepageSection {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SectionType sectionType;
    
    @Column(name = "product_id", nullable = false)
    private Long productId;
    
    @Column(name = "display_order")
    private Integer displayOrder = 0;
    
    public enum SectionType {
        BEST_SELLERS, NEW_ARRIVALS
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public SectionType getSectionType() {
        return sectionType;
    }
    
    public void setSectionType(SectionType sectionType) {
        this.sectionType = sectionType;
    }
    
    public Long getProductId() {
        return productId;
    }
    
    public void setProductId(Long productId) {
        this.productId = productId;
    }
    
    public Integer getDisplayOrder() {
        return displayOrder;
    }
    
    public void setDisplayOrder(Integer displayOrder) {
        this.displayOrder = displayOrder;
    }
}
