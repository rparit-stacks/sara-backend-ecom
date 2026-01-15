package com.sara.ecom.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sara.ecom.dto.AddToCartRequest;
import com.sara.ecom.dto.CartDto;
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
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    public CartDto getCart(String userEmail) {
        List<CartItem> items = cartItemRepository.findByUserEmailOrderByCreatedAtDesc(userEmail);
        
        CartDto cart = new CartDto();
        cart.setItems(items.stream().map(this::toCartItemDto).collect(Collectors.toList()));
        cart.setItemCount(items.size());
        
        BigDecimal subtotal = items.stream()
                .map(CartItem::getTotalPrice)
                .filter(p -> p != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        cart.setSubtotal(subtotal);
        cart.setShipping(calculateShipping(subtotal));
        cart.setTotal(subtotal.add(cart.getShipping()));
        
        return cart;
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
        
        return toCartItemDto(cartItemRepository.save(item));
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
    
    private BigDecimal calculateShipping(BigDecimal subtotal) {
        // Free shipping over 1000
        if (subtotal.compareTo(new BigDecimal("1000")) >= 0) {
            return BigDecimal.ZERO;
        }
        return new BigDecimal("99");
    }
    
    private CartDto.CartItemDto toCartItemDto(CartItem item) {
        CartDto.CartItemDto dto = new CartDto.CartItemDto();
        dto.setId(item.getId());
        dto.setProductType(item.getProductType().name());
        dto.setProductId(item.getProductId());
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
