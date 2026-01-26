package com.sara.ecom.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "product_variants")
public class ProductVariant {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;
    
    @Column(nullable = false)
    private String type; // e.g., "size", "color"
    
    @Column(nullable = false)
    private String name; // e.g., "Size", "Color"
    
    private String unit; // e.g., "cm", "kg"
    
    @Column(name = "frontend_id")
    private String frontendId; // To link with combinations
    
    @Column(name = "display_order")
    private Integer displayOrder = 0;
    
    @OneToMany(mappedBy = "variant", cascade = CascadeType.ALL, orphanRemoval = true)
    @Fetch(FetchMode.SUBSELECT)
    @OrderBy("displayOrder ASC")
    private List<ProductVariantOption> options = new ArrayList<>();
    
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
    
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public String getFrontendId() {
        return frontendId;
    }

    public void setFrontendId(String frontendId) {
        this.frontendId = frontendId;
    }
    
    public Integer getDisplayOrder() {
        return displayOrder;
    }
    
    public void setDisplayOrder(Integer displayOrder) {
        this.displayOrder = displayOrder != null ? displayOrder : 0;
    }
    
    public List<ProductVariantOption> getOptions() {
        return options;
    }
    
    public void setOptions(List<ProductVariantOption> options) {
        this.options = options;
    }
    
    public void addOption(ProductVariantOption option) {
        options.add(option);
        option.setVariant(this);
    }
    
    public void removeOption(ProductVariantOption option) {
        options.remove(option);
        option.setVariant(null);
    }
}
