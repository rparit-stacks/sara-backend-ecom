package com.sara.ecom.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "custom_config_variants")
public class CustomConfigVariant {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "config_id", nullable = false)
    private CustomProductConfig config;
    
    @Column(nullable = false)
    private String type; // e.g., "size", "color"
    
    @Column(nullable = false)
    private String name; // e.g., "Size", "Color"
    
    private String unit; // e.g., "cm", "kg"
    
    @Column(name = "frontend_id")
    private String frontendId;
    
    @OneToMany(mappedBy = "variant", cascade = CascadeType.ALL, orphanRemoval = true)
    @Fetch(FetchMode.SUBSELECT)
    private List<CustomConfigVariantOption> options = new ArrayList<>();
    
    @Column(name = "display_order")
    private Integer displayOrder = 0;
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public CustomProductConfig getConfig() {
        return config;
    }
    
    public void setConfig(CustomProductConfig config) {
        this.config = config;
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
    
    public List<CustomConfigVariantOption> getOptions() {
        return options;
    }
    
    public void setOptions(List<CustomConfigVariantOption> options) {
        this.options = options;
    }
    
    public Integer getDisplayOrder() {
        return displayOrder;
    }
    
    public void setDisplayOrder(Integer displayOrder) {
        this.displayOrder = displayOrder;
    }
    
    public void addOption(CustomConfigVariantOption option) {
        options.add(option);
        option.setVariant(this);
    }
    
    public void removeOption(CustomConfigVariantOption option) {
        options.remove(option);
        option.setVariant(null);
    }
}
