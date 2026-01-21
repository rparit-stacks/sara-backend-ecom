package com.sara.ecom.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "order_items")
public class OrderItem {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;
    
    @Column(name = "product_type")
    private String productType;
    
    @Column(name = "product_id")
    private Long productId;
    
    @Column(nullable = false)
    private String name;
    
    private String image;
    
    @Column(precision = 10, scale = 2)
    private BigDecimal price;
    
    private Integer quantity;
    
    @Column(name = "total_price", precision = 10, scale = 2)
    private BigDecimal totalPrice;
    
    @Column(name = "variants_json", columnDefinition = "TEXT")
    private String variantsJson;
    
    @Column(name = "custom_data_json", columnDefinition = "TEXT")
    private String customDataJson;
    
    // For DESIGNED products
    @Column(name = "design_id")
    private Long designId;
    
    @Column(name = "fabric_id")
    private Long fabricId;
    
    // For DIGITAL products - stored ZIP download URL
    @Column(name = "digital_download_url", columnDefinition = "TEXT")
    private String digitalDownloadUrl;
    
    // For DIGITAL products - ZIP password
    @Column(name = "zip_password", columnDefinition = "TEXT")
    private String zipPassword;
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Order getOrder() {
        return order;
    }
    
    public void setOrder(Order order) {
        this.order = order;
    }
    
    public String getProductType() {
        return productType;
    }
    
    public void setProductType(String productType) {
        this.productType = productType;
    }
    
    public Long getProductId() {
        return productId;
    }
    
    public void setProductId(Long productId) {
        this.productId = productId;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getImage() {
        return image;
    }
    
    public void setImage(String image) {
        this.image = image;
    }
    
    public BigDecimal getPrice() {
        return price;
    }
    
    public void setPrice(BigDecimal price) {
        this.price = price;
    }
    
    public Integer getQuantity() {
        return quantity;
    }
    
    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }
    
    public BigDecimal getTotalPrice() {
        return totalPrice;
    }
    
    public void setTotalPrice(BigDecimal totalPrice) {
        this.totalPrice = totalPrice;
    }
    
    public String getVariantsJson() {
        return variantsJson;
    }
    
    public void setVariantsJson(String variantsJson) {
        this.variantsJson = variantsJson;
    }
    
    public String getCustomDataJson() {
        return customDataJson;
    }
    
    public void setCustomDataJson(String customDataJson) {
        this.customDataJson = customDataJson;
    }
    
    public Long getDesignId() {
        return designId;
    }
    
    public void setDesignId(Long designId) {
        this.designId = designId;
    }
    
    public Long getFabricId() {
        return fabricId;
    }
    
    public void setFabricId(Long fabricId) {
        this.fabricId = fabricId;
    }
    
    public String getDigitalDownloadUrl() {
        return digitalDownloadUrl;
    }
    
    public void setDigitalDownloadUrl(String digitalDownloadUrl) {
        this.digitalDownloadUrl = digitalDownloadUrl;
    }
    
    public String getZipPassword() {
        return zipPassword;
    }
    
    public void setZipPassword(String zipPassword) {
        this.zipPassword = zipPassword;
    }
}
