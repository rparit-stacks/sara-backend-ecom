package com.sara.ecom.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sara.ecom.dto.AddToCartRequest;
import com.sara.ecom.dto.CartDto;
import com.sara.ecom.dto.CouponDto;
import com.sara.ecom.dto.EmailTemplateData;
import com.sara.ecom.entity.User;
import com.sara.ecom.entity.CartItem;
import com.sara.ecom.repository.CartItemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CartService {
    
    @Autowired
    private CartItemRepository cartItemRepository;
    
    @Autowired
    private ShippingService shippingService;
    
    @Autowired
    private CouponService couponService;
    
    @Autowired
    private com.sara.ecom.repository.ProductRepository productRepository;
    
    @Autowired
    private com.sara.ecom.service.CustomProductService customProductService;
    
    @Autowired
    private EmailService emailService;
    
    @Autowired
    private com.sara.ecom.repository.UserRepository userRepository;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    public CartDto getCart(String userEmail, String state, String couponCode) {
        List<CartItem> items = cartItemRepository.findByUserEmailOrderByCreatedAtDesc(userEmail);
        
        CartDto cart = new CartDto();
        cart.setItems(items.stream().map(item -> toCartItemDto(item)).collect(Collectors.toList()));
        cart.setItemCount(items.size());
        
        BigDecimal subtotal = items.stream()
                .map(CartItem::getTotalPrice)
                .filter(p -> p != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        cart.setSubtotal(subtotal);
        
        // Calculate GST per item based on category
        BigDecimal totalGst = BigDecimal.ZERO;
        for (CartDto.CartItemDto item : cart.getItems()) {
            BigDecimal itemGst = calculateItemGst(item);
            totalGst = totalGst.add(itemGst);
        }
        cart.setGst(totalGst);
        
        // Calculate shipping based on state
        BigDecimal shipping = shippingService.calculateShipping(subtotal, state);
        cart.setShipping(shipping);
        
        // Calculate total before coupon: Subtotal + GST + Shipping
        BigDecimal totalBeforeCoupon = subtotal.add(totalGst).add(shipping);
        
        // Apply coupon discount if provided
        BigDecimal couponDiscount = BigDecimal.ZERO;
        if (couponCode != null && !couponCode.trim().isEmpty()) {
            // Validate coupon against totalBeforeCoupon (subtotal + shipping)
            CouponDto couponValidation = couponService.validateCoupon(couponCode, totalBeforeCoupon, userEmail);
            if (couponValidation.getValid() != null && couponValidation.getValid()) {
                couponDiscount = couponValidation.getDiscount() != null ? couponValidation.getDiscount() : BigDecimal.ZERO;
                cart.setAppliedCouponCode(couponCode);
            } else {
                // Invalid coupon - don't set it
                cart.setAppliedCouponCode(null);
            }
        } else {
            cart.setAppliedCouponCode(null);
        }
        cart.setCouponDiscount(couponDiscount);
        
        // Final total: (Subtotal + GST + Shipping) - Coupon Discount
        cart.setTotal(totalBeforeCoupon.subtract(couponDiscount));
        
        return cart;
    }
    
    private BigDecimal calculateItemGst(CartDto.CartItemDto item) {
        try {
            // Get product to find GST rate
            com.sara.ecom.entity.Product product = productRepository.findById(item.getProductId())
                    .orElse(null);
            
            if (product == null) {
                return BigDecimal.ZERO;
            }
            
            // Get GST rate from product
            if (product.getGstRate() == null || product.getGstRate().compareTo(BigDecimal.ZERO) == 0) {
                return BigDecimal.ZERO;
            }
            
            // Calculate GST: (item total price * GST rate) / 100
            BigDecimal itemTotal = item.getTotalPrice() != null ? item.getTotalPrice() : BigDecimal.ZERO;
            BigDecimal gstRate = product.getGstRate();
            BigDecimal gstAmount = itemTotal.multiply(gstRate).divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);
            
            // Set GST info on item
            item.setGstRate(gstRate);
            item.setGstAmount(gstAmount);
            
            return gstAmount;
        } catch (Exception e) {
            // If any error occurs, return zero GST
            return BigDecimal.ZERO;
        }
    }
    
    // Overloaded method for backward compatibility
    public CartDto getCart(String userEmail) {
        return getCart(userEmail, null, null);
    }
    
    @Transactional
    public CartDto.CartItemDto addToCart(String userEmail, AddToCartRequest request) {
        CartItem item = new CartItem();
        item.setUserEmail(userEmail);
        item.setProductType(CartItem.ProductType.valueOf(request.getProductType().toUpperCase()));
        item.setProductId(request.getProductId());
        item.setProductName(request.getProductName());
        item.setProductImage(request.getProductImage());
        item.setDesignId(request.getDesignId());
        item.setFabricId(request.getFabricId());
        item.setFabricPrice(request.getFabricPrice());
        item.setDesignPrice(request.getDesignPrice());
        item.setUploadedDesignUrl(request.getUploadedDesignUrl());
        item.setQuantity(request.getQuantity() != null ? request.getQuantity() : 1);
        item.setUnitPrice(request.getUnitPrice());
        
        if (request.getVariants() != null) {
            try {
                item.setVariants(objectMapper.writeValueAsString(request.getVariants()));
            } catch (JsonProcessingException e) {
                item.setVariants("{}");
            }
        }
        
        if (request.getCustomFormData() != null) {
            try {
                item.setCustomFormData(objectMapper.writeValueAsString(request.getCustomFormData()));
            } catch (JsonProcessingException e) {
                item.setCustomFormData("{}");
            }
        }
        
        CartItem savedItem = cartItemRepository.save(item);
        
        // If this is a custom product (CUSTOM type), save it to CustomProduct table
        if (item.getProductType() == CartItem.ProductType.CUSTOM && request.getCustomProductId() != null) {
            try {
                // Mark the custom product as saved
                customProductService.saveCustomProduct(request.getCustomProductId(), userEmail);
            } catch (Exception e) {
                // Log but don't fail cart addition
                System.err.println("Failed to save custom product: " + e.getMessage());
            }
        }
        
        // Send email notification if user is logged in (not a guest)
        if (userEmail != null && !userEmail.startsWith("guest_")) {
            try {
                User user = userRepository.findByEmail(userEmail).orElse(null);
                if (user != null) {
                    String recipientName = (user.getFirstName() != null ? user.getFirstName() : "") + 
                                         (user.getLastName() != null ? " " + user.getLastName() : "");
                    if (recipientName.trim().isEmpty()) {
                        recipientName = user.getEmail();
                    }
                    
                    EmailTemplateData.CartEmailData emailData = new EmailTemplateData.CartEmailData();
                    emailData.setRecipientName(recipientName.trim());
                    emailData.setRecipientEmail(user.getEmail());
                    emailData.setProductName(request.getProductName());
                    emailData.setProductImage(request.getProductImage());
                    emailData.setPrice(request.getUnitPrice() != null ? request.getUnitPrice() : BigDecimal.ZERO);
                    emailData.setQuantity(request.getQuantity() != null ? request.getQuantity() : 1);
                    emailData.setProductType(request.getProductType());
                    
                    emailService.sendItemAddedToCartEmail(emailData);
                }
            } catch (Exception e) {
                // Log error but don't fail cart addition
                System.err.println("Failed to send cart email: " + e.getMessage());
                e.printStackTrace();
            }
        }
        
        return toCartItemDto(savedItem);
    }
    
    @Transactional
    public CartDto.CartItemDto updateCartItem(String userEmail, Long itemId, Integer quantity) {
        CartItem item = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Cart item not found"));
        
        if (!item.getUserEmail().equals(userEmail)) {
            throw new RuntimeException("Unauthorized access to cart item");
        }
        
        item.setQuantity(quantity);
        return toCartItemDto(cartItemRepository.save(item));
    }
    
    @Transactional
    public void removeFromCart(String userEmail, Long itemId) {
        CartItem item = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Cart item not found"));
        
        if (!item.getUserEmail().equals(userEmail)) {
            throw new RuntimeException("Unauthorized access to cart item");
        }
        
        cartItemRepository.delete(item);
    }
    
    @Transactional
    public void clearCart(String userEmail) {
        cartItemRepository.deleteByUserEmail(userEmail);
    }
    
    public long getCartItemCount(String userEmail) {
        return cartItemRepository.countByUserEmail(userEmail);
    }
    
    private CartDto.CartItemDto toCartItemDto(CartItem item) {
        CartDto.CartItemDto dto = new CartDto.CartItemDto();
        dto.setId(item.getId());
        dto.setProductType(item.getProductType().name());
        dto.setProductId(item.getProductId());
        
        // Fetch product slug for proper routing (especially for custom products)
        try {
            com.sara.ecom.entity.Product product = productRepository.findById(item.getProductId()).orElse(null);
            if (product != null && product.getSlug() != null && !product.getSlug().trim().isEmpty()) {
                dto.setProductSlug(product.getSlug());
            }
        } catch (Exception e) {
            // If product not found or error, slug will remain null
        }
        
        dto.setProductName(item.getProductName());
        dto.setProductImage(item.getProductImage());
        dto.setDesignId(item.getDesignId());
        dto.setFabricId(item.getFabricId());
        dto.setFabricPrice(item.getFabricPrice());
        dto.setDesignPrice(item.getDesignPrice());
        dto.setUploadedDesignUrl(item.getUploadedDesignUrl());
        dto.setQuantity(item.getQuantity());
        dto.setUnitPrice(item.getUnitPrice());
        dto.setTotalPrice(item.getTotalPrice());
        
        // GST will be calculated in getCart method
        dto.setGstRate(BigDecimal.ZERO);
        dto.setGstAmount(BigDecimal.ZERO);
        
        if (item.getVariants() != null && !item.getVariants().isEmpty()) {
            try {
                dto.setVariants(objectMapper.readValue(item.getVariants(), new TypeReference<Map<String, String>>() {}));
            } catch (JsonProcessingException e) {
                dto.setVariants(new HashMap<>());
            }
        }
        
        if (item.getCustomFormData() != null && !item.getCustomFormData().isEmpty()) {
            try {
                dto.setCustomFormData(objectMapper.readValue(item.getCustomFormData(), new TypeReference<Map<String, Object>>() {}));
            } catch (JsonProcessingException e) {
                dto.setCustomFormData(new HashMap<>());
            }
        }
        
        return dto;
    }
}
