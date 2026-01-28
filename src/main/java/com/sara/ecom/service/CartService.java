package com.sara.ecom.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sara.ecom.dto.AddToCartRequest;
import com.sara.ecom.dto.CartDto;
import com.sara.ecom.dto.CouponDto;
import com.sara.ecom.dto.EmailTemplateData;
import com.sara.ecom.dto.VariantSelectionDto;
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
        
        // Store variants - prefer new structured format, fallback to legacy format
        if (request.getVariantSelections() != null && !request.getVariantSelections().isEmpty()) {
            try {
                // Store structured variant selections
                item.setVariants(objectMapper.writeValueAsString(request.getVariantSelections()));
            } catch (JsonProcessingException e) {
                item.setVariants("{}");
            }
        } else if (request.getVariants() != null) {
            // Legacy format support for backward compatibility
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

        // #region agent log
        try {
            java.util.Map<String, Object> payload = new java.util.HashMap<>();
            payload.put("sessionId", "debug-session");
            payload.put("runId", "pre-fix");
            payload.put("hypothesisId", "H1");
            payload.put("location", "CartService.java:138");
            payload.put("message", "addToCart input and derived pricing");

            java.util.Map<String, Object> data = new java.util.HashMap<>();
            data.put("productType", request.getProductType());
            data.put("productId", request.getProductId());
            data.put("designPrice", request.getDesignPrice());
            data.put("fabricPrice", request.getFabricPrice());
            data.put("unitPrice", request.getUnitPrice());
            data.put("quantity", request.getQuantity());

            data.put("entityUnitPrice", item.getUnitPrice());
            data.put("entityQuantity", item.getQuantity());

            payload.put("data", data);
            payload.put("timestamp", System.currentTimeMillis());

            String json = objectMapper.writeValueAsString(payload);
            java.nio.file.Files.write(
                java.nio.file.Paths.get("r:\\My Projects\\Quout\\.cursor\\debug.log"),
                (json + System.lineSeparator()).getBytes(java.nio.charset.StandardCharsets.UTF_8),
                java.nio.file.StandardOpenOption.CREATE,
                java.nio.file.StandardOpenOption.APPEND
            );
        } catch (Exception ignored) {
        }
        // #endregion

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

    /**
     * Full update of a CUSTOM cart item. Replaces design, fabric, variants, form, quantity, and prices.
     * Only allowed when the existing item's productType is CUSTOM.
     */
    @Transactional
    public CartDto.CartItemDto updateCartItemFull(String userEmail, Long itemId, AddToCartRequest request) {
        CartItem item = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Cart item not found"));

        if (!item.getUserEmail().equals(userEmail)) {
            throw new RuntimeException("Unauthorized access to cart item");
        }

        if (item.getProductType() != CartItem.ProductType.CUSTOM) {
            throw new RuntimeException("Full update is only allowed for CUSTOM cart items");
        }

        item.setProductId(request.getProductId() != null ? request.getProductId() : item.getProductId());
        item.setProductName(request.getProductName() != null ? request.getProductName() : item.getProductName());
        item.setProductImage(request.getProductImage() != null ? request.getProductImage() : item.getProductImage());
        item.setDesignId(request.getDesignId());
        item.setFabricId(request.getFabricId());
        item.setFabricPrice(request.getFabricPrice());
        item.setDesignPrice(request.getDesignPrice());
        item.setUploadedDesignUrl(request.getUploadedDesignUrl() != null ? request.getUploadedDesignUrl() : item.getUploadedDesignUrl());
        item.setQuantity(request.getQuantity() != null ? request.getQuantity() : 1);
        item.setUnitPrice(request.getUnitPrice());

        if (request.getVariantSelections() != null && !request.getVariantSelections().isEmpty()) {
            try {
                item.setVariants(objectMapper.writeValueAsString(request.getVariantSelections()));
            } catch (JsonProcessingException e) {
                item.setVariants("{}");
            }
        } else if (request.getVariants() != null) {
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

        // #region agent log
        try {
            java.util.Map<String, Object> payload = new java.util.HashMap<>();
            payload.put("sessionId", "debug-session");
            payload.put("runId", "pre-fix");
            payload.put("hypothesisId", "H2");
            payload.put("location", "CartService.java:311");
            payload.put("message", "toCartItemDto pricing snapshot");

            java.util.Map<String, Object> data = new java.util.HashMap<>();
            data.put("cartItemId", item.getId());
            data.put("productType", item.getProductType() != null ? item.getProductType().name() : null);
            data.put("unitPrice", item.getUnitPrice());
            data.put("quantity", item.getQuantity());
            data.put("totalPrice", item.getTotalPrice());

            payload.put("data", data);
            payload.put("timestamp", System.currentTimeMillis());

            String json = objectMapper.writeValueAsString(payload);
            java.nio.file.Files.write(
                java.nio.file.Paths.get("r:\\My Projects\\Quout\\.cursor\\debug.log"),
                (json + System.lineSeparator()).getBytes(java.nio.charset.StandardCharsets.UTF_8),
                java.nio.file.StandardOpenOption.CREATE,
                java.nio.file.StandardOpenOption.APPEND
            );
        } catch (Exception ignored) {
        }
        // #endregion

        // GST will be calculated in getCart method
        dto.setGstRate(BigDecimal.ZERO);
        dto.setGstAmount(BigDecimal.ZERO);
        
        // Parse variants - try structured format first, fallback to legacy format
        if (item.getVariants() != null && !item.getVariants().isEmpty()) {
            try {
                // Try to parse as structured format (Map<String, VariantSelectionDto>)
                Map<String, VariantSelectionDto> structuredVariants = objectMapper.readValue(
                    item.getVariants(), 
                    new TypeReference<Map<String, VariantSelectionDto>>() {}
                );
                if (structuredVariants != null && !structuredVariants.isEmpty()) {
                    dto.setVariantSelections(structuredVariants);
                    // Also populate legacy format for backward compatibility
                    Map<String, String> legacyVariants = new HashMap<>();
                    for (Map.Entry<String, VariantSelectionDto> entry : structuredVariants.entrySet()) {
                        VariantSelectionDto selection = entry.getValue();
                        if (selection != null && selection.getOptionValue() != null) {
                            // Use frontendId as key if available, otherwise use variantId
                            String key = selection.getVariantFrontendId() != null 
                                ? selection.getVariantFrontendId() 
                                : String.valueOf(selection.getVariantId());
                            legacyVariants.put(key, selection.getOptionValue());
                        }
                    }
                    dto.setVariants(legacyVariants);
                } else {
                    // Fallback to legacy format
                    dto.setVariants(objectMapper.readValue(item.getVariants(), new TypeReference<Map<String, String>>() {}));
                }
            } catch (JsonProcessingException e) {
                // If structured format fails, try legacy format
                try {
                    dto.setVariants(objectMapper.readValue(item.getVariants(), new TypeReference<Map<String, String>>() {}));
                } catch (JsonProcessingException e2) {
                    dto.setVariants(new HashMap<>());
                }
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
