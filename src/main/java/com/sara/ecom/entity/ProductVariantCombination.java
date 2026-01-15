package com.sara.ecom.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Entity
@Table(name = "product_variant_combinations")
public class ProductVariantCombination {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;
    
    @ElementCollection
    @CollectionTable(name = "product_variant_combination_values", 
                    joinColumns = @JoinColumn(name = "combination_id"))
    @MapKeyColumn(name = "variant_frontend_id")
    @Column(name = "option_frontend_id")
    private Map<String, String> variantValues = new HashMap<>(); // variantFrontendId -> optionFrontendId
    
    @Column(precision = 10, scale = 2)
    private BigDecimal price;
    
    private Integer stock;
    
    private String sku;
    
    @Column(name = "frontend_id")
    private String frontendId;

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

    public Map<String, String> getVariantValues() {
        return variantValues;
    }

    public void setVariantValues(Map<String, String> variantValues) {
        this.variantValues = variantValues;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public Integer getStock() {
        return stock;
    }

    public void setStock(Integer stock) {
        this.stock = stock;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public String getFrontendId() {
        return frontendId;
    }

    public void setFrontendId(String frontendId) {
        this.frontendId = frontendId;
    }
}
