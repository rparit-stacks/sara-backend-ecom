package com.sara.ecom.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sara.ecom.dto.AddToCartRequest;
import com.sara.ecom.dto.AddressRequest;
import com.sara.ecom.dto.CartDto;
import com.sara.ecom.dto.CreateOrderRequest;
import com.sara.ecom.dto.EmailTemplateData;
import com.sara.ecom.dto.OrderDto;
import com.sara.ecom.dto.UserAddressDto;
import com.sara.ecom.entity.Order;
import com.sara.ecom.entity.OrderItem;
import com.sara.ecom.entity.User;
import com.sara.ecom.repository.OrderRepository;
import com.sara.ecom.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
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
    
    @Autowired
    private SwipeService swipeService;
    
    @Autowired
    private UserAddressService userAddressService;
    
    @Autowired
    private CategoryService categoryService;
    
    @Autowired
    private com.sara.ecom.repository.CategoryRepository categoryRepository;
    
    @Autowired
    private com.sara.ecom.repository.ProductRepository productRepository;
    
    @Autowired
    private EmailService emailService;
    
    @Autowired
    private com.sara.ecom.service.WhatsAppNotificationService whatsAppNotificationService;
    
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
        
        // Validate category restrictions for all cart items
        for (CartDto.CartItemDto item : cart.getItems()) {
            if (item.getProductId() != null) {
                // Get product's category
                com.sara.ecom.entity.Product product = productRepository.findById(item.getProductId())
                        .orElse(null);
                if (product != null && product.getCategoryId() != null) {
                    // Check if user has access to this category
                    com.sara.ecom.entity.Category category = categoryRepository.findById(product.getCategoryId())
                            .orElse(null);
                    if (category != null && !isCategoryAccessible(category, userEmail)) {
                        throw new RuntimeException(
                            "You do not have permission to purchase products from category: " + category.getName() + 
                            ". This category is restricted to specific users only."
                        );
                    }
                }
            }
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
        
        // Auto-save shipping address to user profile if not using existing address ID
        // This applies to both logged-in users (without default address) and guest users
        if (request.getShippingAddressId() == null && request.getShippingAddress() != null) {
            try {
                Map<String, Object> shippingAddr = request.getShippingAddress();
                AddressRequest addressRequest = new AddressRequest();
                addressRequest.setFirstName((String) shippingAddr.get("firstName"));
                addressRequest.setLastName((String) shippingAddr.get("lastName"));
                addressRequest.setPhoneNumber((String) shippingAddr.get("phone"));
                addressRequest.setAddress((String) shippingAddr.get("address"));
                addressRequest.setCity((String) shippingAddr.get("city"));
                addressRequest.setState((String) shippingAddr.get("state"));
                addressRequest.setZipCode((String) shippingAddr.get("postalCode"));
                addressRequest.setCountry("India"); // Default for Indian addresses
                addressRequest.setGstin((String) shippingAddr.get("gstin"));
                
                // Check if user has any addresses - if not, set as default
                List<UserAddressDto> existingAddresses = userAddressService.getUserAddresses(userEmail);
                addressRequest.setIsDefault(existingAddresses.isEmpty());
                
                userAddressService.createAddress(userEmail, addressRequest);
            } catch (Exception e) {
                // Log error but don't fail order creation
                System.err.println("Failed to save address to user profile: " + e.getMessage());
            }
        }
        
        // Clear cart
        cartService.clearCart(userEmail);
        
        OrderDto orderDto = toOrderDto(saved);
        
        // Send order placed email
        try {
            EmailTemplateData.OrderEmailData emailData = buildOrderEmailData(orderDto, user);
            emailService.sendOrderPlacedEmail(emailData);
        } catch (Exception e) {
            // Log error but don't fail order creation
            System.err.println("Failed to send order placed email: " + e.getMessage());
            e.printStackTrace();
        }
        
        // Send WhatsApp notification for order placed
        try {
            whatsAppNotificationService.sendOrderStatusNotification(
                    saved,
                    com.sara.ecom.entity.OrderStatusTemplate.StatusType.ORDER_PLACED
            );
        } catch (Exception e) {
            // Log error but don't fail order creation
            System.err.println("Failed to send WhatsApp order notification: " + e.getMessage());
            e.printStackTrace();
        }
        
        return orderDto;
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
        
        Order.OrderStatus oldStatus = order.getStatus();
        Order.OrderStatus newStatus = Order.OrderStatus.valueOf(status.toUpperCase());
        order.setStatus(newStatus);
        
        Order savedOrder = orderRepository.save(order);
        
        // If order is being confirmed and Swipe is enabled, create invoice
        if (oldStatus != Order.OrderStatus.CONFIRMED && newStatus == Order.OrderStatus.CONFIRMED) {
            try {
                com.sara.ecom.dto.SwipeDto.SwipeInvoiceResponse swipeResponse = swipeService.createInvoice(savedOrder);
                if (swipeResponse != null && swipeResponse.getSuccess() != null && swipeResponse.getSuccess()) {
                    if (swipeResponse.getData() != null) {
                        savedOrder.setSwipeInvoiceId(swipeResponse.getData().getHashId());
                        savedOrder.setSwipeInvoiceNumber(swipeResponse.getData().getSerialNumber());
                        savedOrder.setSwipeIrn(swipeResponse.getData().getIrn());
                        savedOrder.setSwipeQrCode(swipeResponse.getData().getQrCode());
                        savedOrder.setSwipeInvoiceUrl(swipeResponse.getData().getPdfUrl());
                        savedOrder = orderRepository.save(savedOrder);
                    }
                }
            } catch (Exception e) {
                // Log error but don't fail order update
                System.err.println("Error creating Swipe invoice: " + e.getMessage());
                e.printStackTrace();
            }
        }
        
        OrderDto orderDto = toOrderDto(savedOrder);
        
        // Send order status update email
        try {
            User user = userRepository.findByEmail(savedOrder.getUserEmail()).orElse(null);
            if (user != null) {
                EmailTemplateData.OrderEmailData emailData = buildOrderEmailData(orderDto, user);
                emailData.setOrderStatus(newStatus.name());
                
                switch (newStatus) {
                    case CONFIRMED:
                        emailService.sendOrderConfirmedEmail(emailData);
                        break;
                    case PROCESSING:
                        emailService.sendOrderProcessingEmail(emailData);
                        break;
                    case SHIPPED:
                        emailService.sendOrderShippedEmail(emailData);
                        break;
                    case DELIVERED:
                        emailService.sendOrderDeliveredEmail(emailData);
                        break;
                    case CANCELLED:
                        emailService.sendOrderCancelledEmail(emailData);
                        break;
                    default:
                        break;
                }
            }
        } catch (Exception e) {
            // Log error but don't fail order update
            System.err.println("Failed to send order status email: " + e.getMessage());
            e.printStackTrace();
        }
        
        // Send WhatsApp notification for order status update
        try {
            com.sara.ecom.entity.OrderStatusTemplate.StatusType whatsappStatusType = null;
            switch (newStatus) {
                case CONFIRMED:
                    whatsappStatusType = com.sara.ecom.entity.OrderStatusTemplate.StatusType.ORDER_CONFIRMED;
                    break;
                case PROCESSING:
                    // Processing is same as confirmed, no separate notification
                    break;
                case SHIPPED:
                    whatsappStatusType = com.sara.ecom.entity.OrderStatusTemplate.StatusType.ORDER_SHIPPED;
                    break;
                case DELIVERED:
                    whatsappStatusType = com.sara.ecom.entity.OrderStatusTemplate.StatusType.DELIVERED;
                    break;
                case CANCELLED:
                    whatsappStatusType = com.sara.ecom.entity.OrderStatusTemplate.StatusType.CANCELLED;
                    break;
                default:
                    break;
            }
            
            if (whatsappStatusType != null) {
                whatsAppNotificationService.sendOrderStatusNotification(savedOrder, whatsappStatusType);
            }
        } catch (Exception e) {
            // Log error but don't fail order update
            System.err.println("Failed to send WhatsApp order status notification: " + e.getMessage());
            e.printStackTrace();
        }
        
        return orderDto;
    }
    
    /**
     * Checks if a category is accessible to a user based on email restrictions.
     */
    private boolean isCategoryAccessible(com.sara.ecom.entity.Category category, String userEmail) {
        // If no email restriction, category is accessible
        if (category.getAllowedEmails() == null || category.getAllowedEmails().trim().isEmpty()) {
            return true;
        }
        
        // If user email is null, category is not accessible
        if (userEmail == null || userEmail.trim().isEmpty()) {
            return false;
        }
        
        // Check if user email is in the allowed emails list
        String[] allowedEmails = category.getAllowedEmails().split(",");
        for (String email : allowedEmails) {
            if (email.trim().equalsIgnoreCase(userEmail.trim())) {
                return true;
            }
        }
        
        return false;
    }
    
    @Transactional
    public OrderDto updatePaymentStatus(Long orderId, String paymentStatus, String paymentId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        Order.PaymentStatus oldPaymentStatus = order.getPaymentStatus();
        order.setPaymentStatus(Order.PaymentStatus.valueOf(paymentStatus.toUpperCase()));
        if (paymentId != null) {
            order.setPaymentId(paymentId);
        }
        Order savedOrder = orderRepository.save(order);
        OrderDto orderDto = toOrderDto(savedOrder);
        
        // Send payment status email if status changed
        if (oldPaymentStatus != order.getPaymentStatus()) {
            try {
                User user = userRepository.findByEmail(savedOrder.getUserEmail()).orElse(null);
                if (user != null) {
                    EmailTemplateData.OrderEmailData emailData = buildOrderEmailData(orderDto, user);
                    emailData.setPaymentStatus(paymentStatus);
                    
                    switch (order.getPaymentStatus()) {
                        case PENDING:
                            emailService.sendPaymentPendingEmail(emailData);
                            break;
                        case PAID:
                            emailService.sendPaymentSuccessfulEmail(emailData);
                            break;
                        case FAILED:
                            emailService.sendPaymentFailedEmail(emailData);
                            break;
                        case REFUNDED:
                            emailService.sendPaymentRefundedEmail(emailData);
                            break;
                        default:
                            break;
                    }
                }
            } catch (Exception e) {
                // Log error but don't fail payment status update
                System.err.println("Failed to send payment status email: " + e.getMessage());
                e.printStackTrace();
            }
            
            // Send WhatsApp notification for payment status update
            try {
                com.sara.ecom.entity.OrderStatusTemplate.StatusType whatsappStatusType = null;
                switch (order.getPaymentStatus()) {
                    case PAID:
                        whatsappStatusType = com.sara.ecom.entity.OrderStatusTemplate.StatusType.PAYMENT_SUCCESS;
                        break;
                    case FAILED:
                        whatsappStatusType = com.sara.ecom.entity.OrderStatusTemplate.StatusType.PAYMENT_FAILED;
                        break;
                    case REFUNDED:
                        whatsappStatusType = com.sara.ecom.entity.OrderStatusTemplate.StatusType.REFUND_COMPLETED;
                        break;
                    default:
                        break;
                }
                
                if (whatsappStatusType != null) {
                    whatsAppNotificationService.sendOrderStatusNotification(savedOrder, whatsappStatusType);
                }
            } catch (Exception e) {
                // Log error but don't fail payment status update
                System.err.println("Failed to send WhatsApp payment status notification: " + e.getMessage());
                e.printStackTrace();
            }
        }
        
        return orderDto;
    }
    
    @Transactional
    public OrderDto retrySwipeInvoice(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        
        try {
            com.sara.ecom.dto.SwipeDto.SwipeInvoiceResponse swipeResponse = swipeService.createInvoice(order);
            if (swipeResponse != null && swipeResponse.getSuccess() != null && swipeResponse.getSuccess()) {
                if (swipeResponse.getData() != null) {
                    order.setSwipeInvoiceId(swipeResponse.getData().getHashId());
                    order.setSwipeInvoiceNumber(swipeResponse.getData().getSerialNumber());
                    order.setSwipeIrn(swipeResponse.getData().getIrn());
                    order.setSwipeQrCode(swipeResponse.getData().getQrCode());
                    order.setSwipeInvoiceUrl(swipeResponse.getData().getPdfUrl());
                    order = orderRepository.save(order);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Error creating Swipe invoice: " + e.getMessage(), e);
        }
        
        return toOrderDto(order);
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
        dto.setSwipeInvoiceId(order.getSwipeInvoiceId());
        dto.setSwipeInvoiceNumber(order.getSwipeInvoiceNumber());
        dto.setSwipeIrn(order.getSwipeIrn());
        dto.setSwipeQrCode(order.getSwipeQrCode());
        dto.setSwipeInvoiceUrl(order.getSwipeInvoiceUrl());
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
    
    /**
     * Builds OrderEmailData from OrderDto and User for email notifications
     */
    private EmailTemplateData.OrderEmailData buildOrderEmailData(OrderDto orderDto, User user) {
        String recipientName = (user.getFirstName() != null ? user.getFirstName() : "") + 
                               (user.getLastName() != null ? " " + user.getLastName() : "");
        if (recipientName.trim().isEmpty()) {
            recipientName = user.getEmail();
        }
        
        // Format shipping address
        String shippingAddressStr = "";
        if (orderDto.getShippingAddress() != null) {
            Map<String, Object> addr = orderDto.getShippingAddress();
            shippingAddressStr = String.format("%s, %s, %s %s, %s",
                addr.getOrDefault("address", ""),
                addr.getOrDefault("city", ""),
                addr.getOrDefault("state", ""),
                addr.getOrDefault("postalCode", ""),
                addr.getOrDefault("country", "India")
            );
        }
        
        // Format billing address
        String billingAddressStr = "";
        if (orderDto.getBillingAddress() != null) {
            Map<String, Object> addr = orderDto.getBillingAddress();
            billingAddressStr = String.format("%s, %s, %s %s, %s",
                addr.getOrDefault("address", ""),
                addr.getOrDefault("city", ""),
                addr.getOrDefault("state", ""),
                addr.getOrDefault("postalCode", ""),
                addr.getOrDefault("country", "India")
            );
        }
        
        // Build order items
        List<EmailTemplateData.OrderItemData> orderItems = new ArrayList<>();
        if (orderDto.getItems() != null) {
            for (OrderDto.OrderItemDto item : orderDto.getItems()) {
                orderItems.add(EmailTemplateData.OrderItemData.builder()
                    .productName(item.getName())
                    .productImage(item.getImage())
                    .quantity(item.getQuantity())
                    .unitPrice(item.getPrice())
                    .totalPrice(item.getTotalPrice())
                    .productType(item.getProductType())
                    .build());
            }
        }
        
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");
        String orderDate = orderDto.getCreatedAt() != null 
            ? orderDto.getCreatedAt().format(formatter) 
            : LocalDateTime.now().format(formatter);
        
        EmailTemplateData.OrderEmailData emailData = new EmailTemplateData.OrderEmailData();
        // Base fields
        emailData.setRecipientName(recipientName.trim());
        emailData.setRecipientEmail(user.getEmail());
        // Order specific fields
        emailData.setOrderNumber(orderDto.getOrderNumber());
        emailData.setOrderDate(orderDate);
        emailData.setOrderStatus(orderDto.getStatus());
        emailData.setPaymentStatus(orderDto.getPaymentStatus());
        emailData.setSubtotal(orderDto.getSubtotal());
        emailData.setGst(orderDto.getGst());
        emailData.setShipping(orderDto.getShipping());
        emailData.setTotal(orderDto.getTotal());
        emailData.setCouponDiscount(orderDto.getCouponDiscount());
        emailData.setCouponCode(orderDto.getCouponCode());
        emailData.setShippingAddress(shippingAddressStr);
        emailData.setBillingAddress(billingAddressStr);
        emailData.setItems(orderItems);
        emailData.setInvoiceUrl(orderDto.getSwipeInvoiceUrl());
        return emailData;
    }
}
