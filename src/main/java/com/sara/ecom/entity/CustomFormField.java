package com.sara.ecom.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "custom_form_fields")
public class CustomFormField {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String type; // text, number, select, checkbox, textarea
    
    @Column(nullable = false)
    private String label;
    
    private String placeholder;
    
    @Column(nullable = false)
    private Boolean required = false;
    
    @Column(name = "min_value")
    private Integer minValue;
    
    @Column(name = "max_value")
    private Integer maxValue;
    
    @Column(columnDefinition = "TEXT")
    private String options; // JSON array for select options
    
    @Column(name = "display_order")
    private Integer displayOrder = 0;
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    public String getLabel() {
        return label;
    }
    
    public void setLabel(String label) {
        this.label = label;
    }
    
    public String getPlaceholder() {
        return placeholder;
    }
    
    public void setPlaceholder(String placeholder) {
        this.placeholder = placeholder;
    }
    
    public Boolean getRequired() {
        return required;
    }
    
    public void setRequired(Boolean required) {
        this.required = required;
    }
    
    public Integer getMinValue() {
        return minValue;
    }
    
    public void setMinValue(Integer minValue) {
        this.minValue = minValue;
    }
    
    public Integer getMaxValue() {
        return maxValue;
    }
    
    public void setMaxValue(Integer maxValue) {
        this.maxValue = maxValue;
    }
    
    public String getOptions() {
        return options;
    }
    
    public void setOptions(String options) {
        this.options = options;
    }
    
    public Integer getDisplayOrder() {
        return displayOrder;
    }
    
    public void setDisplayOrder(Integer displayOrder) {
        this.displayOrder = displayOrder;
    }
}
