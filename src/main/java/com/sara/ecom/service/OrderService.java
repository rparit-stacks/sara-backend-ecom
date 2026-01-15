package com.sara.ecom.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sara.ecom.dto.AddToCartRequest;
import com.sara.ecom.dto.CartDto;
import com.sara.ecom.dto.CreateOrderRequest;
import com.sara.ecom.dto.OrderDto;
import com.sara.ecom.entity.Order;
import com.sara.ecom.entity.OrderItem;
import com.sara.ecom.entity.User;
import com.sara.ecom.repository.OrderRepository;
import com.sara.ecom.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class OrderService {
    
    @Autowired
    private OrderRepository orderRepository;
    
    @Autowired
    private CartService cartService;
    
    @Autowired
    private CouponService couponService;
    
    @Autowired
    private ShippingService shippingService;
    
    @Autowired
    private UserRepository userRepository;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Transactional
    public OrderDto createOrder(String userEmail, CreateOrderRequest request) {
        // Handle guest checkout - auto-create user if needed
        User user;
        if (userEmail == null || userEmail.isEmpty()) {
            // Guest checkout - create user from request data
            if (request.getGuestEmail() == null || request.getGuestEmail().isEmpty()) {
                throw new RuntimeException("Email is required for guest checkout");
            }
            
            // Convert email to lowercase to prevent duplicate accounts
            String normalizedEmail = request.getGuestEmail().toLowerCase().trim();
            user = userRepository.findByEmail(normalizedEmail)
                    .orElseGet(() -> {
                        // Create new user for guest checkout
                        User newUser = User.builder()
                                .email(normalizedEmail)
                                .firstName(request.getGuestFirstName())
                                .lastName(request.getGuestLastName())
                                .phoneNumber(request.getGuestPhone())
                                .authProvider(User.AuthProvider.OTP) // Default for guest users
                                .emailVerified(false)
                                .status(User.UserStatus.ACTIVE)
                                .build();
                        return userRepository.save(newUser);
                    });
            
            // Update user info if provided and not set
            boolean updated = false;
            if (request.getGuestFirstName() != null && user.getFirstName() == null) {
                user.setFirstName(request.getGuestFirstName());
                updated = true;
            }
            if (request.getGuestLastName() != null && user.getLastName() == null) {
                user.setLastName(request.getGuestLastName());
                updated = true;
            }
            if (request.getGuestPhone() != null && user.getPhoneNumber() == null) {
                user.setPhoneNumber(request.getGuestPhone());
                updated = true;
            }
            if (updated) {
                user = userRepository.save(user);
            }
            
            userEmail = user.getEmail();
            
            // For guest checkout, create cart items from request if provided
            if (request.getGuestCartItems() != null && !request.getGuestCartItems().isEmpty()) {
                for (AddToCartRequest cartItemRequest : request.getGuestCartItems()) {
                    cartService.addToCart(userEmail, cartItemRequest);
                }
            }
        } else {
            // Existing user
            user = userRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new RuntimeException("User not found"));
        }
        
        // Get cart items (without state/coupon for initial calculation)
        CartDto cart = cartService.getCart(userEmail, null, null);
        if (cart.getItems().isEmpty()) {
            throw new RuntimeException("Cart is empty");
        }
        
        // Extract state from shipping address
        String state = null;
        if (request.getShippingAddress() != null && request.getShippingAddress().containsKey("state")) {
            state = (String) request.getShippingAddress().get("state");
        } else if (request.getShippingAddressId() != null) {
            // Try to get state from address ID if available
            // This would require UserAddressService - for now use address from request
        }
        
        // Recalculate shipping based on address state
        BigDecimal shipping = shippingService.calculateShipping(cart.getSubtotal(), state);
        
        // Generate unique 7-digit random order ID (1000000 to 9999999)
        Long orderId = 1000000L + (long)(Math.random() * 9000000L);
        boolean isUnique = false;
        int attempts = 0;
        while (!isUnique && attempts < 100) {
            if (!orderRepository.existsById(orderId)) {
                isUnique = true;
            } else {
                attempts++;
                orderId = 1000000L + (long)(Math.random() * 9000000L);
            }
        }
        if (!isUnique) {
            // Fallback: use timestamp-based ID if all random attempts fail
            orderId = System.currentTimeMillis() % 10000000L;
            if (orderId < 1000000L) {
                orderId += 1000000L;
            }
            // Ensure it's unique by incrementing if needed
            while (orderRepository.existsById(orderId)) {
                orderId = (orderId + 1L) % 10000000L;
                if (orderId < 1000000L) {
                    orderId += 1000000L;
                }
            }
        }
        
        // Create order
        Order order = new Order();
        order.setId(orderId);
        order.setUserEmail(user.getEmail());
        order.setUserName(user.getFirstName() + " " + (user.getLastName() != null ? user.getLastName() : ""));
        order.setSubtotal(cart.getSubtotal());
        order.setGst(cart.getGst() != null ? cart.getGst() : BigDecimal.ZERO);
        order.setShipping(shipping);
        order.setPaymentMethod(request.getPaymentMethod());
        order.setNotes(request.getNotes());
        order.setShippingAddressId(request.getShippingAddressId());
        
        // Set addresses
        if (request.getShippingAddress() != null) {
            try {
                order.setShippingAddress(objectMapper.writeValueAsString(request.getShippingAddress()));
            } catch (JsonProcessingException e) {
                order.setShippingAddress("{}");
            }
        }
        
        if (request.getBillingAddress() != null) {
            try {
                order.setBillingAddress(objectMapper.writeValueAsString(request.getBillingAddress()));
            } catch (JsonProcessingException e) {
                order.setBillingAddress("{}");
            }
        }
        
        // Apply coupon if provided
        BigDecimal couponDiscount = BigDecimal.ZERO;
        if (request.getCouponCode() != null && !request.getCouponCode().isEmpty()) {
            // Validate against subtotal + GST + shipping
            BigDecimal totalBeforeCoupon = cart.getSubtotal()
                    .add(cart.getGst() != null ? cart.getGst() : BigDecimal.ZERO)
                    .add(shipping);
            var validation = couponService.validateCoupon(request.getCouponCode(), totalBeforeCoupon, userEmail);
            if (validation.getValid() != null && validation.getValid()) {
                couponDiscount = validation.getDiscount() != null ? validation.getDiscount() : BigDecimal.ZERO;
                order.setCouponCode(request.getCouponCode());
                order.setCouponDiscount(couponDiscount);
                couponService.useCoupon(request.getCouponCode(), userEmail);
            }
        }
        
        // Calculate total: (Subtotal + GST + Shipping) - Coupon Discount
        BigDecimal gst = cart.getGst() != null ? cart.getGst() : BigDecimal.ZERO;
        order.setTotal(cart.getSubtotal().add(gst).add(shipping).subtract(couponDiscount));
        
        // Add items
        for (CartDto.CartItemDto cartItem : cart.getItems()) {
            OrderItem orderItem = new OrderItem();
            orderItem.setProductType(cartItem.getProductType());
            orderItem.setProductId(cartItem.getProductId());
            orderItem.setName(cartItem.getProductName());
            orderItem.setImage(cartItem.getProductImage());
            orderItem.setPrice(cartItem.getUnitPrice());
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setTotalPrice(cartItem.getTotalPrice());
            orderItem.setDesignId(cartItem.getDesignId());
            orderItem.setFabricId(cartItem.getFabricId());
            
            if (cartItem.getVariants() != null) {
                try {
                    orderItem.setVariantsJson(objectMapper.writeValueAsString(cartItem.getVariants()));
                } catch (JsonProcessingException e) {
                    orderItem.setVariantsJson("{}");
                }
            }
            
            if (cartItem.getCustomFormData() != null) {
                try {
                    orderItem.setCustomDataJson(objectMapper.writeValueAsString(cartItem.getCustomFormData()));
                } catch (JsonProcessingException e) {
                    orderItem.setCustomDataJson("{}");
                }
            }
            
            order.addItem(orderItem);
        }
        
        Order saved = orderRepository.save(order);
        
        // Clear cart
        cartService.clearCart(userEmail);
        
        return toOrderDto(saved);
    }
    
    public List<OrderDto> getUserOrders(String userEmail) {
        return orderRepository.findByUserEmailOrderByCreatedAtDesc(userEmail).stream()
                .map(this::toOrderDto)
                .collect(Collectors.toList());
    }
    
    public OrderDto getOrderById(Long orderId, String userEmail) {
        Order order = orderRepository.findByIdAndUserEmailWithItems(orderId, userEmail)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        return toOrderDto(order);
    }
    
    // Public method to get order by ID (for confirmation page - no auth required)
    public OrderDto getOrderByIdPublic(Long orderId) {
        Order order = orderRepository.findByIdWithItems(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        return toOrderDto(order);
    }
    
    // Admin methods
    public List<OrderDto> getAllOrders(String status) {
        List<Order> orders;
        if (status != null) {
            orders = orderRepository.findByStatusOrderByCreatedAtDesc(Order.OrderStatus.valueOf(status.toUpperCase()));
        } else {
            orders = orderRepository.findAllByOrderByCreatedAtDesc();
        }
        return orders.stream().map(this::toOrderDto).collect(Collectors.toList());
    }
    
    public OrderDto getOrderByIdAdmin(Long orderId) {
        Order order = orderRepository.findByIdWithItems(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        return toOrderDto(order);
    }
    
    @Transactional
    public OrderDto updateOrderStatus(Long orderId, String status) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        order.setStatus(Order.OrderStatus.valueOf(status.toUpperCase()));
        return toOrderDto(orderRepository.save(order));
    }
    
    @Transactional
    public OrderDto updatePaymentStatus(Long orderId, String paymentStatus, String paymentId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        order.setPaymentStatus(Order.PaymentStatus.valueOf(paymentStatus.toUpperCase()));
        if (paymentId != null) {
            order.setPaymentId(paymentId);
        }
        return toOrderDto(orderRepository.save(order));
    }
    
    private OrderDto toOrderDto(Order order) {
        OrderDto dto = new OrderDto();
        dto.setId(order.getId());
        dto.setOrderNumber(order.getOrderNumber());
        dto.setUserEmail(order.getUserEmail());
        dto.setUserName(order.getUserName());
        dto.setSubtotal(order.getSubtotal());
        dto.setGst(order.getGst());
        dto.setShipping(order.getShipping());
        dto.setTotal(order.getTotal());
        dto.setCouponCode(order.getCouponCode());
        dto.setCouponDiscount(order.getCouponDiscount());
        dto.setStatus(order.getStatus().name());
        dto.setPaymentStatus(order.getPaymentStatus().name());
        dto.setPaymentMethod(order.getPaymentMethod());
        dto.setNotes(order.getNotes());
        dto.setCreatedAt(order.getCreatedAt());
        
        // Parse addresses
        if (order.getShippingAddress() != null) {
            try {
                dto.setShippingAddress(objectMapper.readValue(order.getShippingAddress(), new TypeReference<Map<String, Object>>() {}));
            } catch (JsonProcessingException e) {
                dto.setShippingAddress(new HashMap<>());
            }
        }
        
        if (order.getBillingAddress() != null) {
            try {
                dto.setBillingAddress(objectMapper.readValue(order.getBillingAddress(), new TypeReference<Map<String, Object>>() {}));
            } catch (JsonProcessingException e) {
                dto.setBillingAddress(new HashMap<>());
            }
        }
        
        // Items
        if (order.getItems() != null) {
            dto.setItems(order.getItems().stream().map(this::toOrderItemDto).collect(Collectors.toList()));
        }
        
        return dto;
    }
    
    private OrderDto.OrderItemDto toOrderItemDto(OrderItem item) {
        OrderDto.OrderItemDto dto = new OrderDto.OrderItemDto();
        dto.setId(item.getId());
        dto.setProductType(item.getProductType());
        dto.setProductId(item.getProductId());
        dto.setName(item.getName());
        dto.setImage(item.getImage());
        dto.setPrice(item.getPrice());
        dto.setQuantity(item.getQuantity());
        dto.setTotalPrice(item.getTotalPrice());
        dto.setDesignId(item.getDesignId());
        dto.setFabricId(item.getFabricId());
        
        if (item.getVariantsJson() != null) {
            try {
                dto.setVariants(objectMapper.readValue(item.getVariantsJson(), new TypeReference<Map<String, String>>() {}));
            } catch (JsonProcessingException e) {
                dto.setVariants(new HashMap<>());
            }
        }
        
        if (item.getCustomDataJson() != null) {
            try {
                dto.setCustomData(objectMapper.readValue(item.getCustomDataJson(), new TypeReference<Map<String, Object>>() {}));
            } catch (JsonProcessingException e) {
                dto.setCustomData(new HashMap<>());
            }
        }
        
        return dto;
    }
}
