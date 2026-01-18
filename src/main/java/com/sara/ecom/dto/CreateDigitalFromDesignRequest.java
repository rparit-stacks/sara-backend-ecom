package com.sara.ecom.dto;

import java.math.BigDecimal;

/**
 * Simple DTO for creating a Digital Product from a Design Product.
 * This is used when user clicks "Buy Design Only" button.
 */
public class CreateDigitalFromDesignRequest {
    private BigDecimal price;
    
    public CreateDigitalFromDesignRequest() {
    }
    
    public CreateDigitalFromDesignRequest(BigDecimal price) {
        this.price = price;
    }
    
    public BigDecimal getPrice() {
        return price;
    }
    
    public void setPrice(BigDecimal price) {
        this.price = price;
    }
}
