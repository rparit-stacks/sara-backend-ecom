package com.sara.ecom.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "plain_product_variants")
public class PlainProductVariant {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plain_product_id", nullable = false)
    private PlainProduct plainProduct;
    
    @Column(nullable = false)
    private String type; // e.g., "width", "gsm"
    
    @Column(nullable = false)
    private String name; // e.g., "Width", "GSM"
    
    @OneToMany(mappedBy = "variant", cascade = CascadeType.ALL, orphanRemoval = true)
    @Fetch(FetchMode.SUBSELECT)
    @OrderBy("displayOrder ASC")
    private List<PlainProductVariantOption> options = new ArrayList<>();
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public PlainProduct getPlainProduct() {
        return plainProduct;
    }
    
    public void setPlainProduct(PlainProduct plainProduct) {
        this.plainProduct = plainProduct;
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
    
    public List<PlainProductVariantOption> getOptions() {
        return options;
    }
    
    public void setOptions(List<PlainProductVariantOption> options) {
        this.options = options;
    }
    
    public void addOption(PlainProductVariantOption option) {
        options.add(option);
        option.setVariant(this);
    }
    
    public void removeOption(PlainProductVariantOption option) {
        options.remove(option);
        option.setVariant(null);
    }
}
