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
import com.sara.ecom.dto.VariantSelectionDto;
import com.sara.ecom.dto.VariantDisplayInfo;
import com.sara.ecom.entity.Order;
import com.sara.ecom.entity.OrderItem;
import com.sara.ecom.entity.OrderPaymentHistory;
import com.sara.ecom.entity.PaymentConfig;
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
    private com.sara.ecom.repository.CategoryRepository categoryRepository;
    
    @Autowired
    private com.sara.ecom.repository.ProductRepository productRepository;
    
    @Autowired
    private EmailService emailService;
    
    @Autowired
    private NotificationHooks notificationHooks;
    
    @Autowired
    private ProductService productService;
    
    @Autowired
    private PaymentConfigService paymentConfigService;
    
    @Autowired
    private com.sara.ecom.repository.OrderPaymentHistoryRepository paymentHistoryRepository;
    
    @Autowired
    private com.sara.ecom.repository.OrderAuditLogRepository auditLogRepository;
    
    @Autowired
    private JwtService jwtService;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * Log an audit entry for order changes (public for controller use)
     */
    public void logAuditEntry(Long orderId, String changedBy, String changeType, 
                               String fieldName, String oldValue, String newValue, String changeReason) {
        try {
            com.sara.ecom.entity.OrderAuditLog log = new com.sara.ecom.entity.OrderAuditLog();
            log.setOrderId(orderId);
            log.setChangedBy(changedBy != null ? changedBy : "system");
            log.setChangeType(changeType);
            log.setFieldName(fieldName);
            log.setOldValue(oldValue);
            log.setNewValue(newValue);
            log.setChangeReason(changeReason);
            auditLogRepository.save(log);
        } catch (Exception e) {
            // Don't fail the operation if audit logging fails
            System.err.println("Failed to log audit entry: " + e.getMessage());
        }
    }
    
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
        
        // Validate payment method for digital products
        boolean hasDigitalProducts = cart.getItems().stream()
                .anyMatch(item -> "DIGITAL".equals(item.getProductType()));
        
        if (hasDigitalProducts) {
            String paymentMethod = request.getPaymentMethod();
            if (paymentMethod == null || paymentMethod.trim().isEmpty()) {
                throw new RuntimeException("Digital products require online payment only. Payment method is required.");
            }
            String paymentMethodLower = paymentMethod.toLowerCase();
            if ("cod".equals(paymentMethodLower) || "cash_on_delivery".equals(paymentMethodLower)) {
                throw new RuntimeException("Digital products require online payment only. COD is not available.");
            }
            // Also check for partial COD
            if (paymentMethodLower.contains("partial") && paymentMethodLower.contains("cod")) {
                throw new RuntimeException("Digital products require online payment only. Partial COD is not available.");
            }
        }
        
        // Validate email and phone are present (mandatory for order processing)
        if (user.getEmail() == null || user.getEmail().trim().isEmpty()) {
            throw new RuntimeException("Email is required for order processing");
        }
        String phoneNumber = user.getPhoneNumber();
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            // Try to get from shipping address
            if (request.getShippingAddress() != null) {
                try {
                    Map<String, Object> shippingAddr = request.getShippingAddress();
                    phoneNumber = (String) shippingAddr.get("phone");
                } catch (Exception e) {
                    // Ignore
                }
            }
            if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
                throw new RuntimeException("Mobile number is required for order processing");
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
        BigDecimal orderTotal = cart.getSubtotal().add(gst).add(shipping).subtract(couponDiscount);
        order.setTotal(orderTotal);
        
        // Check PaymentConfig for partial COD settings
        PaymentConfig paymentConfig = paymentConfigService.getConfigEntity();
        
        // For digital products, always use full online payment (ignore COD settings)
        if (!hasDigitalProducts && paymentConfig.getPartialCodEnabled() != null && paymentConfig.getPartialCodEnabled()) {
            // Partial COD: Calculate advance payment
            Integer advancePercentage = paymentConfig.getPartialCodAdvancePercentage();
            if (advancePercentage != null && advancePercentage >= 10 && advancePercentage <= 90) {
                BigDecimal advanceAmount = orderTotal.multiply(BigDecimal.valueOf(advancePercentage))
                        .divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);
                
                // Store advance amount as payment amount
                order.setPaymentAmount(advanceAmount);
            } else {
                // Invalid percentage, use full amount
                order.setPaymentAmount(orderTotal);
            }
        } else {
            // Full payment (either full COD, online payment, or digital products)
            order.setPaymentAmount(orderTotal);
        }
        
        // Default payment currency to INR; gateway-specific handlers may override
        if (order.getPaymentAmount() != null) {
            order.setPaymentCurrency("INR");
        }
        
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
            
            // ZIP generation for digital products will happen after payment is completed
            // Do not generate ZIP here - wait for payment status = PAID
            
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
        
        // Trigger notification hook (currently no-op, WhatsApp removed)
        notificationHooks.onOrderPlaced(saved);
        
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
        return updateOrderStatus(orderId, status, null, null, false, "system");
    }
    
    public OrderDto updateOrderStatus(Long orderId, String status, boolean skipWhatsApp) {
        return updateOrderStatus(orderId, status, null, null, skipWhatsApp, "system");
    }
    
    public OrderDto updateOrderStatus(Long orderId, String status, String customStatus, String customMessage, boolean skipWhatsApp) {
        return updateOrderStatus(orderId, status, customStatus, customMessage, skipWhatsApp, "system");
    }
    
    public OrderDto updateOrderStatus(Long orderId, String status, String customStatus, String customMessage, boolean skipWhatsApp, String changedBy) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        
        Order.OrderStatus oldStatus = order.getStatus();
        String oldStatusString = oldStatus != null ? oldStatus.name() : null;
        Order.OrderStatus newStatus = Order.OrderStatus.valueOf(status.toUpperCase());
        order.setStatus(newStatus);
        
        // Log audit entry
        if (oldStatus != newStatus) {
            logAuditEntry(orderId, changedBy, "STATUS_UPDATE", "status", oldStatusString, newStatus.name(), 
                         customMessage != null ? customMessage : "Status updated");
        }
        
        // Handle cancellation - set cancellation fields if status is CANCELLED
        if (newStatus == Order.OrderStatus.CANCELLED && oldStatus != Order.OrderStatus.CANCELLED) {
            // If cancelling for the first time, set cancelled_at timestamp
            if (order.getCancelledAt() == null) {
                order.setCancelledAt(LocalDateTime.now());
            }
            // cancelled_by and cancellation_reason should be set via separate endpoint if needed
        }
        
        // Set custom status if provided
        if (customStatus != null && !customStatus.trim().isEmpty()) {
            String oldCustomStatus = order.getCustomStatus();
            order.setCustomStatus(customStatus.trim());
            if (!customStatus.trim().equals(oldCustomStatus)) {
                logAuditEntry(orderId, changedBy, "STATUS_UPDATE", "customStatus", oldCustomStatus, customStatus.trim(), 
                             customMessage != null ? customMessage : "Custom status updated");
            }
        } else {
            // Clear custom status when setting standard status
            String oldCustomStatus = order.getCustomStatus();
            if (oldCustomStatus != null) {
                order.setCustomStatus(null);
                logAuditEntry(orderId, changedBy, "STATUS_UPDATE", "customStatus", oldCustomStatus, null, 
                             "Custom status cleared");
            }
        }
        
        Order savedOrder = orderRepository.save(order);
        
        // If order is being confirmed and Swipe is enabled, create invoice
        // BUT: Only create if invoice_status is NOT_CREATED (prevent duplicates)
        if (oldStatus != Order.OrderStatus.CONFIRMED && newStatus == Order.OrderStatus.CONFIRMED) {
            // Check invoice status - if already CREATED, skip invoice creation
            if (savedOrder.getInvoiceStatus() == null || 
                savedOrder.getInvoiceStatus() == Order.InvoiceStatus.NOT_CREATED) {
                // Invoice not created yet - create it now
                try {
                    com.sara.ecom.dto.SwipeDto.SwipeInvoiceResponse swipeResponse = swipeService.createInvoice(savedOrder);
                    if (swipeResponse != null && swipeResponse.getSuccess() != null && swipeResponse.getSuccess()) {
                        // Mark invoice as CREATED first (even if data is null, invoice was created)
                        savedOrder.setInvoiceStatus(Order.InvoiceStatus.CREATED);
                        savedOrder.setInvoiceCreatedAt(java.time.LocalDateTime.now());
                        
                        // Update invoice details if data is available
                        if (swipeResponse.getData() != null) {
                            savedOrder.setSwipeInvoiceId(swipeResponse.getData().getHashId());
                            savedOrder.setSwipeInvoiceNumber(swipeResponse.getData().getSerialNumber());
                            savedOrder.setSwipeIrn(swipeResponse.getData().getIrn());
                            savedOrder.setSwipeQrCode(swipeResponse.getData().getQrCode());
                            savedOrder.setSwipeInvoiceUrl(swipeResponse.getData().getPdfUrl());
                        }
                        
                        savedOrder = orderRepository.save(savedOrder);
                    }
                } catch (Exception e) {
                    // Log error but don't fail order update
                    System.err.println("Error creating Swipe invoice: " + e.getMessage());
                    e.printStackTrace();
                }
            } else {
                // Invoice already CREATED - skip creation, just proceed with order confirmation
                // This handles the fail-safe recovery scenario:
                // Invoice was created earlier, but order confirmation failed (e.g., WhatsApp failed)
                // Now on retry, we skip duplicate invoice creation and proceed with confirmation
                System.out.println("Invoice already created for order " + savedOrder.getId() + 
                    ", skipping duplicate creation. Proceeding with order confirmation.");
                
                // Also handle legacy orders: if swipeInvoiceId exists but status is NOT_CREATED,
                // update status to CREATED (migration for old orders)
                if (savedOrder.getSwipeInvoiceId() != null && !savedOrder.getSwipeInvoiceId().trim().isEmpty() &&
                    (savedOrder.getInvoiceStatus() == null || 
                     savedOrder.getInvoiceStatus() == Order.InvoiceStatus.NOT_CREATED)) {
                    savedOrder.setInvoiceStatus(Order.InvoiceStatus.CREATED);
                    if (savedOrder.getInvoiceCreatedAt() == null) {
                        savedOrder.setInvoiceCreatedAt(java.time.LocalDateTime.now());
                    }
                    savedOrder = orderRepository.save(savedOrder);
                    System.out.println("Updated invoice status to CREATED for legacy order " + savedOrder.getId());
                }
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
        
        // Trigger notification hook
        String newStatusString = newStatus.name();
        notificationHooks.onOrderStatusChanged(savedOrder, oldStatusString, newStatusString, customMessage, skipWhatsApp);
        
        return orderDto;
    }
    
    /**
     * Update order with custom status
     */
    @Transactional
    public OrderDto updateCustomStatus(Long orderId, String customStatus, String customMessage, boolean skipWhatsApp) {
        return updateCustomStatus(orderId, customStatus, customMessage, skipWhatsApp, "system");
    }
    
    @Transactional
    public OrderDto updateCustomStatus(Long orderId, String customStatus, String customMessage, boolean skipWhatsApp, String changedBy) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        
        String oldStatusString = order.getStatus() != null ? order.getStatus().name() : null;
        String oldCustomStatus = order.getCustomStatus();
        
        order.setCustomStatus(customStatus != null ? customStatus.trim() : null);
        
        // Log audit entry
        if (!customStatus.trim().equals(oldCustomStatus)) {
            logAuditEntry(orderId, changedBy, "STATUS_UPDATE", "customStatus", oldCustomStatus, customStatus.trim(), 
                         customMessage != null ? customMessage : "Custom status updated");
        }
        
        Order savedOrder = orderRepository.save(order);
        OrderDto orderDto = toOrderDto(savedOrder);
        
        // Send order status update email (optional - can be customized)
        try {
            User user = userRepository.findByEmail(savedOrder.getUserEmail()).orElse(null);
            if (user != null) {
                EmailTemplateData.OrderEmailData emailData = buildOrderEmailData(orderDto, user);
                emailData.setOrderStatus(customStatus != null ? customStatus : savedOrder.getStatus().name());
                // You can add custom email template here if needed
            }
        } catch (Exception e) {
            System.err.println("Failed to send order status email: " + e.getMessage());
            e.printStackTrace();
        }
        
        // Trigger notification hook
        String newStatusString = customStatus != null ? customStatus : (savedOrder.getStatus() != null ? savedOrder.getStatus().name() : "");
        notificationHooks.onOrderStatusChanged(savedOrder, oldStatusString, newStatusString, customMessage, skipWhatsApp);
        
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
        return updatePaymentStatus(orderId, paymentStatus, paymentId, null, "system");
    }
    
    @Transactional
    public OrderDto updatePaymentStatus(Long orderId, String paymentStatus, String paymentId, BigDecimal paymentAmount) {
        return updatePaymentStatus(orderId, paymentStatus, paymentId, paymentAmount, "system");
    }
    
    @Transactional
    public OrderDto updatePaymentStatus(Long orderId, String paymentStatus, String paymentId, BigDecimal paymentAmount, String changedBy) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        Order.PaymentStatus oldPaymentStatus = order.getPaymentStatus();
        String oldPaymentId = order.getPaymentId();
        BigDecimal oldPaymentAmount = order.getPaymentAmount();
        
        order.setPaymentStatus(Order.PaymentStatus.valueOf(paymentStatus.toUpperCase()));
        if (paymentId != null) {
            order.setPaymentId(paymentId);
        }
        if (paymentAmount != null) {
            order.setPaymentAmount(paymentAmount);
        }
        
        // Log audit entries
        if (oldPaymentStatus != order.getPaymentStatus()) {
            logAuditEntry(orderId, changedBy, "PAYMENT_UPDATE", "paymentStatus", 
                         oldPaymentStatus != null ? oldPaymentStatus.name() : null, 
                         order.getPaymentStatus().name(), "Payment status updated");
        }
        if (paymentId != null && !paymentId.equals(oldPaymentId)) {
            logAuditEntry(orderId, changedBy, "PAYMENT_UPDATE", "paymentId", oldPaymentId, paymentId, 
                         "Payment transaction ID updated");
        }
        if (paymentAmount != null && !paymentAmount.equals(oldPaymentAmount)) {
            logAuditEntry(orderId, changedBy, "PAYMENT_UPDATE", "paymentAmount", 
                         oldPaymentAmount != null ? oldPaymentAmount.toString() : null, 
                         paymentAmount.toString(), "Payment amount updated");
        }
        
        // Create payment history entry for partial COD or when payment status changes to PAID
        if (order.getPaymentMethod() != null && order.getPaymentMethod().equals("PARTIAL_COD") 
            && paymentAmount != null && paymentAmount.compareTo(BigDecimal.ZERO) > 0) {
            // Check if this is advance payment or remaining payment
            BigDecimal orderTotal = order.getTotal() != null ? order.getTotal() : BigDecimal.ZERO;
            String paymentType = paymentAmount.compareTo(orderTotal) < 0 ? "ADVANCE" : "FULL";
            
            OrderPaymentHistory history = new OrderPaymentHistory();
            history.setOrderId(orderId);
            history.setPaymentType(paymentType);
            history.setAmount(paymentAmount);
            history.setCurrency(order.getPaymentCurrency() != null ? order.getPaymentCurrency() : "INR");
            history.setTransactionId(paymentId);
            history.setPaymentMethod(order.getPaymentMethod());
            history.setPaidAt(LocalDateTime.now());
            history.setNotes("Payment status updated to " + paymentStatus);
            paymentHistoryRepository.save(history);
        } else if (oldPaymentStatus != Order.PaymentStatus.PAID 
                   && order.getPaymentStatus() == Order.PaymentStatus.PAID
                   && paymentAmount != null && paymentAmount.compareTo(BigDecimal.ZERO) > 0) {
            // Create payment history for full payment
            OrderPaymentHistory history = new OrderPaymentHistory();
            history.setOrderId(orderId);
            history.setPaymentType("FULL");
            history.setAmount(paymentAmount);
            history.setCurrency(order.getPaymentCurrency() != null ? order.getPaymentCurrency() : "INR");
            history.setTransactionId(paymentId);
            history.setPaymentMethod(order.getPaymentMethod());
            history.setPaidAt(LocalDateTime.now());
            history.setNotes("Payment completed");
            paymentHistoryRepository.save(history);
        }
        
        Order savedOrder = orderRepository.save(order);
        
        // Generate password-protected ZIP for digital products when payment is completed
        if (order.getPaymentStatus() == Order.PaymentStatus.PAID && oldPaymentStatus != Order.PaymentStatus.PAID) {
            generateDigitalProductZip(savedOrder);
        }
        
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
            
            // Trigger notification hook (currently no-op, WhatsApp removed)
            notificationHooks.onPaymentStatusChanged(savedOrder);
        }
        
        return orderDto;
    }
    
    /**
     * Generates password-protected ZIP files for digital products in an order.
     * Only called when payment status changes to PAID.
     */
    @Transactional
    protected void generateDigitalProductZip(Order order) {
        if (order.getItems() == null || order.getItems().isEmpty()) {
            return;
        }
        
        // ZIP password = user's registered email
        String userEmail = order.getUserEmail();
        User user = userRepository.findByEmail(userEmail).orElse(null);
        if (user == null) {
            System.err.println("User not found for order: " + order.getId());
            return;
        }
        String zipPassword = user.getEmail();
        
        // Process each order item
        for (OrderItem item : order.getItems()) {
            if ("DIGITAL".equals(item.getProductType()) && item.getProductId() != null) {
                try {
                    // Generate password-protected ZIP
                    String zipUrl = productService.generatePasswordProtectedZip(item.getProductId(), zipPassword);
                    
                    // Store ZIP URL and password in order item
                    item.setDigitalDownloadUrl(zipUrl);
                    item.setZipPassword(zipPassword);
                    
                    // Save order item
                    orderRepository.save(order);
                } catch (Exception e) {
                    System.err.println("Failed to generate password-protected ZIP for digital product " + item.getProductId() + " in order " + order.getId() + ": " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }
    }
    
    @Transactional
    public OrderDto retrySwipeInvoice(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        
        try {
            com.sara.ecom.dto.SwipeDto.SwipeInvoiceResponse swipeResponse = swipeService.createInvoice(order);
            if (swipeResponse != null && swipeResponse.getSuccess() != null && swipeResponse.getSuccess()) {
                System.out.print("Mark invoice as CREATED first (even if data is null, invoice was created)");
                order.setInvoiceStatus(Order.InvoiceStatus.CREATED);
                if (order.getInvoiceCreatedAt() == null) {
                    order.setInvoiceCreatedAt(java.time.LocalDateTime.now());
                }
                
                // Update invoice details if data is available
                if (swipeResponse.getData() != null) {
                    order.setSwipeInvoiceId(swipeResponse.getData().getHashId());
                    order.setSwipeInvoiceNumber(swipeResponse.getData().getSerialNumber());
                    order.setSwipeIrn(swipeResponse.getData().getIrn());
                    order.setSwipeQrCode(swipeResponse.getData().getQrCode());
                    order.setSwipeInvoiceUrl(swipeResponse.getData().getPdfUrl());
                }
                
                order = orderRepository.save(order);
            }
        } catch (Exception e) {
            throw new RuntimeException("Error creating Swipe invoice: " + e.getMessage(), e);
        }
        
        return toOrderDto(order);
    }

    @Transactional
    public OrderDto updateOrderNotes(Long orderId, String notes) {
        return updateOrderNotes(orderId, notes, "system");
    }
    
    @Transactional
    public OrderDto updateOrderNotes(Long orderId, String notes, String changedBy) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        String oldNotes = order.getNotes();
        order.setNotes(notes);
        
        // Log audit entry
        if (oldNotes == null || !notes.equals(oldNotes)) {
            logAuditEntry(orderId, changedBy, "NOTES_UPDATE", "notes", oldNotes, notes, "Order notes updated");
        }
        
        Order savedOrder = orderRepository.save(order);
        return toOrderDto(savedOrder);
    }
    
    @Transactional
    public OrderDto updateCancellationInfo(Long orderId, String cancellationReason, String cancelledBy) {
        return updateCancellationInfo(orderId, cancellationReason, cancelledBy, "system");
    }
    
    @Transactional
    public OrderDto updateCancellationInfo(Long orderId, String cancellationReason, String cancelledBy, String changedBy) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        String oldCancellationReason = order.getCancellationReason();
        String oldCancelledBy = order.getCancelledBy();
        
        order.setCancellationReason(cancellationReason);
        order.setCancelledBy(cancelledBy);
        if (order.getCancelledAt() == null && order.getStatus() == Order.OrderStatus.CANCELLED) {
            order.setCancelledAt(LocalDateTime.now());
        }
        
        // Log audit entries
        if (cancellationReason != null && !cancellationReason.equals(oldCancellationReason)) {
            logAuditEntry(orderId, changedBy, "CANCELLATION_UPDATE", "cancellationReason", 
                         oldCancellationReason, cancellationReason, "Cancellation reason updated");
        }
        if (cancelledBy != null && !cancelledBy.equals(oldCancelledBy)) {
            logAuditEntry(orderId, changedBy, "CANCELLATION_UPDATE", "cancelledBy", oldCancelledBy, cancelledBy, 
                         "Cancelled by updated");
        }
        
        Order savedOrder = orderRepository.save(order);
        return toOrderDto(savedOrder);
    }
    
    public List<com.sara.ecom.dto.OrderPaymentHistoryDto> getPaymentHistory(Long orderId) {
        List<OrderPaymentHistory> history = paymentHistoryRepository.findByOrderIdOrderByPaidAtDesc(orderId);
        return history.stream().map(this::toPaymentHistoryDto).collect(Collectors.toList());
    }
    
    public List<com.sara.ecom.dto.OrderAuditLogDto> getAuditLog(Long orderId) {
        List<com.sara.ecom.entity.OrderAuditLog> logs = auditLogRepository.findByOrderIdOrderByCreatedAtDesc(orderId);
        return logs.stream().map(this::toAuditLogDto).collect(Collectors.toList());
    }
    
    private com.sara.ecom.dto.OrderAuditLogDto toAuditLogDto(com.sara.ecom.entity.OrderAuditLog log) {
        com.sara.ecom.dto.OrderAuditLogDto dto = new com.sara.ecom.dto.OrderAuditLogDto();
        dto.setId(log.getId());
        dto.setOrderId(log.getOrderId());
        dto.setChangedBy(log.getChangedBy());
        dto.setChangeType(log.getChangeType());
        dto.setFieldName(log.getFieldName());
        dto.setOldValue(log.getOldValue());
        dto.setNewValue(log.getNewValue());
        dto.setChangeReason(log.getChangeReason());
        dto.setCreatedAt(log.getCreatedAt());
        return dto;
    }
    
    @Transactional
    public com.sara.ecom.dto.OrderPaymentHistoryDto addPaymentHistory(Long orderId, String paymentType,
                                                                       BigDecimal amount, String currency,
                                                                       String transactionId, String paymentMethod,
                                                                       LocalDateTime paidAt, String notes) {
        OrderPaymentHistory history = new OrderPaymentHistory();
        history.setOrderId(orderId);
        history.setPaymentType(paymentType);
        history.setAmount(amount);
        history.setCurrency(currency != null ? currency : "INR");
        history.setTransactionId(transactionId);
        history.setPaymentMethod(paymentMethod);
        history.setPaidAt(paidAt != null ? paidAt : LocalDateTime.now());
        history.setNotes(notes);
        OrderPaymentHistory saved = paymentHistoryRepository.save(history);
        return toPaymentHistoryDto(saved);
    }
    
    private com.sara.ecom.dto.OrderPaymentHistoryDto toPaymentHistoryDto(OrderPaymentHistory history) {
        com.sara.ecom.dto.OrderPaymentHistoryDto dto = new com.sara.ecom.dto.OrderPaymentHistoryDto();
        dto.setId(history.getId());
        dto.setOrderId(history.getOrderId());
        dto.setPaymentType(history.getPaymentType());
        dto.setAmount(history.getAmount());
        dto.setCurrency(history.getCurrency());
        dto.setTransactionId(history.getTransactionId());
        dto.setPaymentMethod(history.getPaymentMethod());
        dto.setPaidAt(history.getPaidAt());
        dto.setNotes(history.getNotes());
        dto.setCreatedAt(history.getCreatedAt());
        return dto;
    }
    
    @Transactional
    public OrderDto updateOrderItem(Long orderId, Long itemId, Integer quantity, BigDecimal price, String name) {
        return updateOrderItem(orderId, itemId, quantity, price, name, "system");
    }
    
    @Transactional
    public OrderDto updateOrderItem(Long orderId, Long itemId, Integer quantity, BigDecimal price, String name, String changedBy) {
        Order order = orderRepository.findByIdWithItems(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        
        OrderItem item = order.getItems().stream()
                .filter(i -> i.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Order item not found"));
        
        Integer oldQuantity = item.getQuantity();
        BigDecimal oldPrice = item.getPrice();
        String oldName = item.getName();
        
        if (quantity != null && quantity > 0) {
            item.setQuantity(quantity);
        }
        if (price != null && price.compareTo(BigDecimal.ZERO) >= 0) {
            item.setPrice(price);
        }
        if (name != null && !name.trim().isEmpty()) {
            item.setName(name.trim());
        }
        
        // Log audit entries
        if (quantity != null && !quantity.equals(oldQuantity)) {
            logAuditEntry(orderId, changedBy, "ITEM_UPDATE", "item_" + itemId + "_quantity", 
                         oldQuantity != null ? oldQuantity.toString() : null, quantity.toString(), 
                         "Item quantity updated");
        }
        if (price != null && !price.equals(oldPrice)) {
            logAuditEntry(orderId, changedBy, "ITEM_UPDATE", "item_" + itemId + "_price", 
                         oldPrice != null ? oldPrice.toString() : null, price.toString(), 
                         "Item price updated");
        }
        if (name != null && !name.trim().equals(oldName)) {
            logAuditEntry(orderId, changedBy, "ITEM_UPDATE", "item_" + itemId + "_name", oldName, name.trim(), 
                         "Item name updated");
        }
        
        // Recalculate item total
        if (item.getQuantity() != null && item.getPrice() != null) {
            item.setTotalPrice(item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
        }
        
        // Recalculate order subtotal
        BigDecimal oldSubtotal = order.getSubtotal();
        BigDecimal newSubtotal = order.getItems().stream()
                .map(OrderItem::getTotalPrice)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        order.setSubtotal(newSubtotal);
        
        // Recalculate order total
        BigDecimal gst = order.getGst() != null ? order.getGst() : BigDecimal.ZERO;
        BigDecimal shipping = order.getShipping() != null ? order.getShipping() : BigDecimal.ZERO;
        BigDecimal couponDiscount = order.getCouponDiscount() != null ? order.getCouponDiscount() : BigDecimal.ZERO;
        BigDecimal oldTotal = order.getTotal();
        BigDecimal newTotal = newSubtotal.add(gst).add(shipping).subtract(couponDiscount);
        order.setTotal(newTotal);
        
        // Log subtotal and total changes
        if (!newSubtotal.equals(oldSubtotal)) {
            logAuditEntry(orderId, changedBy, "PRICE_UPDATE", "subtotal", 
                         oldSubtotal != null ? oldSubtotal.toString() : null, newSubtotal.toString(), 
                         "Subtotal recalculated from items");
        }
        if (!newTotal.equals(oldTotal)) {
            logAuditEntry(orderId, changedBy, "PRICE_UPDATE", "total", 
                         oldTotal != null ? oldTotal.toString() : null, newTotal.toString(), 
                         "Total recalculated");
        }
        
        Order savedOrder = orderRepository.save(order);
        return toOrderDto(savedOrder);
    }
    
    @Transactional
    public OrderDto updateOrderPricing(Long orderId, BigDecimal subtotal, BigDecimal gst, BigDecimal shipping, BigDecimal total) {
        return updateOrderPricing(orderId, subtotal, gst, shipping, total, "system");
    }
    
    @Transactional
    public OrderDto updateOrderPricing(Long orderId, BigDecimal subtotal, BigDecimal gst, BigDecimal shipping, BigDecimal total, String changedBy) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        
        BigDecimal oldSubtotal = order.getSubtotal();
        BigDecimal oldGst = order.getGst();
        BigDecimal oldShipping = order.getShipping();
        BigDecimal oldTotal = order.getTotal();
        
        if (subtotal != null && subtotal.compareTo(BigDecimal.ZERO) >= 0) {
            order.setSubtotal(subtotal);
        }
        if (gst != null && gst.compareTo(BigDecimal.ZERO) >= 0) {
            order.setGst(gst);
        }
        if (shipping != null && shipping.compareTo(BigDecimal.ZERO) >= 0) {
            order.setShipping(shipping);
        }
        
        // Recalculate total if not provided
        if (total == null) {
            BigDecimal calculatedSubtotal = order.getSubtotal() != null ? order.getSubtotal() : BigDecimal.ZERO;
            BigDecimal calculatedGst = order.getGst() != null ? order.getGst() : BigDecimal.ZERO;
            BigDecimal calculatedShipping = order.getShipping() != null ? order.getShipping() : BigDecimal.ZERO;
            BigDecimal couponDiscount = order.getCouponDiscount() != null ? order.getCouponDiscount() : BigDecimal.ZERO;
            total = calculatedSubtotal.add(calculatedGst).add(calculatedShipping).subtract(couponDiscount);
        }
        
        order.setTotal(total);
        
        // Log audit entries
        if (subtotal != null && !subtotal.equals(oldSubtotal)) {
            logAuditEntry(orderId, changedBy, "PRICE_UPDATE", "subtotal", 
                         oldSubtotal != null ? oldSubtotal.toString() : null, subtotal.toString(), 
                         "Subtotal manually updated");
        }
        if (gst != null && !gst.equals(oldGst)) {
            logAuditEntry(orderId, changedBy, "PRICE_UPDATE", "gst", 
                         oldGst != null ? oldGst.toString() : null, gst.toString(), 
                         "GST manually updated");
        }
        if (shipping != null && !shipping.equals(oldShipping)) {
            logAuditEntry(orderId, changedBy, "PRICE_UPDATE", "shipping", 
                         oldShipping != null ? oldShipping.toString() : null, shipping.toString(), 
                         "Shipping charges manually updated");
        }
        if (!total.equals(oldTotal)) {
            logAuditEntry(orderId, changedBy, "PRICE_UPDATE", "total", 
                         oldTotal != null ? oldTotal.toString() : null, total.toString(), 
                         "Total manually updated");
        }
        
        Order savedOrder = orderRepository.save(order);
        return toOrderDto(savedOrder);
    }
    
    @Transactional
    public OrderDto recalculateOrderTotals(Long orderId) {
        return recalculateOrderTotals(orderId, "system");
    }
    
    @Transactional
    public OrderDto recalculateOrderTotals(Long orderId, String changedBy) {
        Order order = orderRepository.findByIdWithItems(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        
        BigDecimal oldSubtotal = order.getSubtotal();
        BigDecimal oldTotal = order.getTotal();
        
        // Recalculate item totals
        for (OrderItem item : order.getItems()) {
            if (item.getQuantity() != null && item.getPrice() != null) {
                item.setTotalPrice(item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
            }
        }
        
        // Recalculate order subtotal from items
        BigDecimal newSubtotal = order.getItems().stream()
                .map(OrderItem::getTotalPrice)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        order.setSubtotal(newSubtotal);
        
        // Recalculate order total
        BigDecimal gst = order.getGst() != null ? order.getGst() : BigDecimal.ZERO;
        BigDecimal shipping = order.getShipping() != null ? order.getShipping() : BigDecimal.ZERO;
        BigDecimal couponDiscount = order.getCouponDiscount() != null ? order.getCouponDiscount() : BigDecimal.ZERO;
        BigDecimal newTotal = newSubtotal.add(gst).add(shipping).subtract(couponDiscount);
        order.setTotal(newTotal);
        
        // Log audit entry
        logAuditEntry(orderId, changedBy, "PRICE_UPDATE", "recalculate", 
                     "Subtotal: " + (oldSubtotal != null ? oldSubtotal.toString() : "null") + 
                     ", Total: " + (oldTotal != null ? oldTotal.toString() : "null"),
                     "Subtotal: " + newSubtotal.toString() + ", Total: " + newTotal.toString(), 
                     "Order totals recalculated");
        
        Order savedOrder = orderRepository.save(order);
        return toOrderDto(savedOrder);
    }
    
    @Transactional
    public OrderDto updateOrderShippingAddressAdmin(Long orderId, Map<String, Object> shippingAddress) {
        return updateOrderShippingAddressAdmin(orderId, shippingAddress, "system");
    }
    
    @Transactional
    public OrderDto updateOrderShippingAddressAdmin(Long orderId, Map<String, Object> shippingAddress, String changedBy) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if (shippingAddress == null) {
            throw new RuntimeException("Shipping address is required");
        }

        // Normalize keys and validate mandatory fields
        Map<String, Object> normalized = new HashMap<>(shippingAddress);

        String phone = (String) normalized.getOrDefault("phone", normalized.get("phoneNumber"));
        String address = (String) normalized.get("address");
        String city = (String) normalized.get("city");
        String state = (String) normalized.get("state");
        String postalCode = (String) normalized.getOrDefault("postalCode", normalized.get("zipCode"));
        String country = (String) normalized.getOrDefault("country", "India");

        if (phone == null || phone.trim().isEmpty()) throw new RuntimeException("Phone is required");
        if (address == null || address.trim().isEmpty()) throw new RuntimeException("Address is required");
        if (city == null || city.trim().isEmpty()) throw new RuntimeException("City is required");
        if (state == null || state.trim().isEmpty()) throw new RuntimeException("State is required");
        if (postalCode == null || postalCode.trim().isEmpty()) throw new RuntimeException("Postal code is required");

        normalized.put("phone", phone.trim());
        normalized.put("address", address.trim());
        normalized.put("city", city.trim());
        normalized.put("state", state.trim());
        normalized.put("country", country != null ? country.trim() : "India");

        // Keep both keys for compatibility across frontend + SwipeService
        normalized.put("postalCode", postalCode.trim());
        normalized.put("zipCode", postalCode.trim());

        // Ensure addressLine2 exists (Swipe expects it; we default empty string)
        Object addressLine2Obj = normalized.getOrDefault("addressLine2", normalized.get("address_line2"));
        String addressLine2 = addressLine2Obj != null ? String.valueOf(addressLine2Obj).trim() : "";
        normalized.put("addressLine2", addressLine2);

        String oldShippingAddress = order.getShippingAddress();
        try {
            order.setShippingAddress(objectMapper.writeValueAsString(normalized));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to save shipping address", e);
        }

        // Also update userName on order if name is provided (helps admin UI)
        String firstName = (String) normalized.get("firstName");
        String lastName = (String) normalized.get("lastName");
        String oldUserName = order.getUserName();
        if (firstName != null && !firstName.trim().isEmpty()) {
            String fullName = firstName.trim() + (lastName != null && !lastName.trim().isEmpty() ? " " + lastName.trim() : "");
            order.setUserName(fullName.trim());
        }
        
        // Log audit entry
        logAuditEntry(orderId, changedBy, "ADDRESS_UPDATE", "shippingAddress", oldShippingAddress, 
                     order.getShippingAddress(), "Shipping address updated");
        if (order.getUserName() != null && !order.getUserName().equals(oldUserName)) {
            logAuditEntry(orderId, changedBy, "ADDRESS_UPDATE", "userName", oldUserName, order.getUserName(), 
                         "User name updated from address");
        }

        Order saved = orderRepository.save(order);
        return toOrderDto(saved);
    }
    
    @Transactional
    public OrderDto updateOrderBillingAddressAdmin(Long orderId, Map<String, Object> billingAddress) {
        return updateOrderBillingAddressAdmin(orderId, billingAddress, "system");
    }
    
    @Transactional
    public OrderDto updateOrderBillingAddressAdmin(Long orderId, Map<String, Object> billingAddress, String changedBy) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if (billingAddress == null) {
            throw new RuntimeException("Billing address is required");
        }

        // Normalize keys and validate mandatory fields
        Map<String, Object> normalized = new HashMap<>(billingAddress);

        String phone = (String) normalized.getOrDefault("phone", normalized.get("phoneNumber"));
        String address = (String) normalized.get("address");
        String city = (String) normalized.get("city");
        String state = (String) normalized.get("state");
        String postalCode = (String) normalized.getOrDefault("postalCode", normalized.get("zipCode"));
        String country = (String) normalized.getOrDefault("country", "India");

        if (phone == null || phone.trim().isEmpty()) throw new RuntimeException("Phone is required");
        if (address == null || address.trim().isEmpty()) throw new RuntimeException("Address is required");
        if (city == null || city.trim().isEmpty()) throw new RuntimeException("City is required");
        if (state == null || state.trim().isEmpty()) throw new RuntimeException("State is required");
        if (postalCode == null || postalCode.trim().isEmpty()) throw new RuntimeException("Postal code is required");

        normalized.put("phone", phone.trim());
        normalized.put("address", address.trim());
        normalized.put("city", city.trim());
        normalized.put("state", state.trim());
        normalized.put("country", country != null ? country.trim() : "India");

        // Keep both keys for compatibility
        normalized.put("postalCode", postalCode.trim());
        normalized.put("zipCode", postalCode.trim());

        // Ensure addressLine2 exists
        Object addressLine2Obj = normalized.getOrDefault("addressLine2", normalized.get("address_line2"));
        String addressLine2 = addressLine2Obj != null ? String.valueOf(addressLine2Obj).trim() : "";
        normalized.put("addressLine2", addressLine2);

        String oldBillingAddress = order.getBillingAddress();
        try {
            order.setBillingAddress(objectMapper.writeValueAsString(normalized));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to save billing address", e);
        }
        
        // Log audit entry
        logAuditEntry(orderId, changedBy, "ADDRESS_UPDATE", "billingAddress", oldBillingAddress, 
                     order.getBillingAddress(), "Billing address updated");

        Order saved = orderRepository.save(order);
        return toOrderDto(saved);
    }
    
 /*   @Transactional
    public OrderDto updateRefundInfo(Long orderId, BigDecimal refundAmount, LocalDateTime refundDate, 
                                     String refundTransactionId, String refundReason) {
        return updateRefundInfo(orderId, refundAmount, refundDate, refundTransactionId, refundReason, "system");
    }
    */
    @Transactional
    public OrderDto updateRefundInfo(Long orderId, BigDecimal refundAmount, LocalDateTime refundDate, 
                                     String refundTransactionId, String refundReason) {
        return updateRefundInfo(orderId, refundAmount, refundDate, refundTransactionId, refundReason, "system");
    }
    
    @Transactional
    public OrderDto updateRefundInfo(Long orderId, BigDecimal refundAmount, LocalDateTime refundDate, 
                                     String refundTransactionId, String refundReason, String changedBy) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        BigDecimal oldRefundAmount = order.getRefundAmount();
        String oldRefundTransactionId = order.getRefundTransactionId();
        String oldRefundReason = order.getRefundReason();
        
        order.setRefundAmount(refundAmount);
        order.setRefundDate(refundDate);
        order.setRefundTransactionId(refundTransactionId);
        order.setRefundReason(refundReason);
        // If refund amount is set, update payment status to REFUNDED
        if (refundAmount != null && refundAmount.compareTo(BigDecimal.ZERO) > 0) {
            order.setPaymentStatus(Order.PaymentStatus.REFUNDED);
            // Create payment history entry for refund
            OrderPaymentHistory refundHistory = new OrderPaymentHistory();
            refundHistory.setOrderId(orderId);
            refundHistory.setPaymentType("REFUND");
            refundHistory.setAmount(refundAmount);
            refundHistory.setCurrency(order.getPaymentCurrency() != null ? order.getPaymentCurrency() : "INR");
            refundHistory.setTransactionId(refundTransactionId);
            refundHistory.setPaymentMethod(order.getPaymentMethod());
            refundHistory.setPaidAt(refundDate != null ? refundDate : LocalDateTime.now());
            refundHistory.setNotes(refundReason);
            paymentHistoryRepository.save(refundHistory);
        }
        
        // Log audit entries
        if (refundAmount != null && !refundAmount.equals(oldRefundAmount)) {
            logAuditEntry(orderId, changedBy, "REFUND_UPDATE", "refundAmount", 
                         oldRefundAmount != null ? oldRefundAmount.toString() : null, refundAmount.toString(), 
                         "Refund amount updated");
        }
        if (refundTransactionId != null && !refundTransactionId.equals(oldRefundTransactionId)) {
            logAuditEntry(orderId, changedBy, "REFUND_UPDATE", "refundTransactionId", oldRefundTransactionId, 
                         refundTransactionId, "Refund transaction ID updated");
        }
        if (refundReason != null && !refundReason.equals(oldRefundReason)) {
            logAuditEntry(orderId, changedBy, "REFUND_UPDATE", "refundReason", oldRefundReason, refundReason, 
                         "Refund reason updated");
        }
        
        Order savedOrder = orderRepository.save(order);
        return toOrderDto(savedOrder);
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
        dto.setCustomStatus(order.getCustomStatus());
        dto.setPaymentStatus(order.getPaymentStatus().name());
        dto.setPaymentMethod(order.getPaymentMethod());
        dto.setPaymentId(order.getPaymentId());
        dto.setNotes(order.getNotes());
        dto.setSwipeInvoiceId(order.getSwipeInvoiceId());
        dto.setSwipeInvoiceNumber(order.getSwipeInvoiceNumber());
        dto.setSwipeIrn(order.getSwipeIrn());
        dto.setSwipeQrCode(order.getSwipeQrCode());
        dto.setSwipeInvoiceUrl(order.getSwipeInvoiceUrl());
        dto.setInvoiceStatus(order.getInvoiceStatus() != null ? order.getInvoiceStatus().name() : "NOT_CREATED");
        dto.setInvoiceCreatedAt(order.getInvoiceCreatedAt());
        dto.setCreatedAt(order.getCreatedAt());
        dto.setPaymentCurrency(order.getPaymentCurrency());
        dto.setPaymentAmount(order.getPaymentAmount());
        dto.setCancellationReason(order.getCancellationReason());
        dto.setCancelledBy(order.getCancelledBy());
        dto.setCancelledAt(order.getCancelledAt());
        dto.setRefundAmount(order.getRefundAmount());
        dto.setRefundDate(order.getRefundDate());
        dto.setRefundTransactionId(order.getRefundTransactionId());
        dto.setRefundReason(order.getRefundReason());
        
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
        dto.setDigitalDownloadUrl(item.getDigitalDownloadUrl());
        dto.setZipPassword(item.getZipPassword());
        
        // Parse variants - try structured format first, fallback to legacy format
        if (item.getVariantsJson() != null && !item.getVariantsJson().isEmpty()) {
            try {
                // Try to parse as structured format (Map<String, VariantSelectionDto>)
                Map<String, VariantSelectionDto> structuredVariants = objectMapper.readValue(
                    item.getVariantsJson(), 
                    new TypeReference<Map<String, VariantSelectionDto>>() {}
                );
                if (structuredVariants != null && !structuredVariants.isEmpty()) {
                    dto.setVariantSelections(structuredVariants);
                    // Also populate legacy format for backward compatibility
                    Map<String, String> legacyVariants = new HashMap<>();
                    for (Map.Entry<String, VariantSelectionDto> entry : structuredVariants.entrySet()) {
                        VariantSelectionDto selection = entry.getValue();
                        if (selection != null && selection.getOptionValue() != null) {
                            String key = selection.getVariantFrontendId() != null 
                                ? selection.getVariantFrontendId() 
                                : String.valueOf(selection.getVariantId());
                            legacyVariants.put(key, selection.getOptionValue());
                        }
                    }
                    dto.setVariants(legacyVariants);
                    
                    // Resolve variant/option names for display
                    List<VariantDisplayInfo> variantDisplay = resolveVariantNames(item.getProductId(), structuredVariants);
                    dto.setVariantDisplay(variantDisplay);
                } else {
                    // Fallback to legacy format
                    dto.setVariants(objectMapper.readValue(item.getVariantsJson(), new TypeReference<Map<String, String>>() {}));
                }
            } catch (JsonProcessingException e) {
                // If structured format fails, try legacy format
                try {
                    dto.setVariants(objectMapper.readValue(item.getVariantsJson(), new TypeReference<Map<String, String>>() {}));
                } catch (JsonProcessingException e2) {
                    dto.setVariants(new HashMap<>());
                }
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
     * Resolves variant and option names from product for display purposes.
     * Uses stored IDs to look up names from the product.
     */
    private List<VariantDisplayInfo> resolveVariantNames(Long productId, Map<String, VariantSelectionDto> variantSelections) {
        List<VariantDisplayInfo> displayList = new ArrayList<>();
        
        if (productId == null || variantSelections == null || variantSelections.isEmpty()) {
            return displayList;
        }
        
        try {
            com.sara.ecom.entity.Product product = productRepository.findById(productId).orElse(null);
            if (product == null || product.getVariants() == null) {
                // If product not found, use data from variantSelections
                for (VariantSelectionDto selection : variantSelections.values()) {
                    if (selection != null) {
                        VariantDisplayInfo info = new VariantDisplayInfo();
                        info.setVariantName(selection.getVariantName() != null ? selection.getVariantName() : "Unknown");
                        info.setVariantType(selection.getVariantType());
                        info.setVariantUnit(selection.getVariantUnit());
                        info.setOptionValue(selection.getOptionValue() != null ? selection.getOptionValue() : "Unknown");
                        info.setPriceModifier(selection.getPriceModifier());
                        displayList.add(info);
                    }
                }
                return displayList;
            }
            
            // Resolve names from product variants
            for (VariantSelectionDto selection : variantSelections.values()) {
                if (selection == null) continue;
                
                VariantDisplayInfo info = new VariantDisplayInfo();
                
                // Find variant by ID or frontendId
                com.sara.ecom.entity.ProductVariant variant = null;
                if (selection.getVariantId() != null) {
                    variant = product.getVariants().stream()
                        .filter(v -> v.getId().equals(selection.getVariantId()))
                        .findFirst()
                        .orElse(null);
                }
                if (variant == null && selection.getVariantFrontendId() != null) {
                    variant = product.getVariants().stream()
                        .filter(v -> selection.getVariantFrontendId().equals(v.getFrontendId()))
                        .findFirst()
                        .orElse(null);
                }
                
                if (variant != null) {
                    info.setVariantName(variant.getName());
                    info.setVariantType(variant.getType());
                    info.setVariantUnit(variant.getUnit());
                    
                    // Find option by ID or frontendId
                    com.sara.ecom.entity.ProductVariantOption option = null;
                    if (selection.getOptionId() != null) {
                        option = variant.getOptions().stream()
                            .filter(o -> o.getId().equals(selection.getOptionId()))
                            .findFirst()
                            .orElse(null);
                    }
                    if (option == null && selection.getOptionFrontendId() != null) {
                        option = variant.getOptions().stream()
                            .filter(o -> selection.getOptionFrontendId().equals(o.getFrontendId()))
                            .findFirst()
                            .orElse(null);
                    }
                    
                    if (option != null) {
                        info.setOptionValue(option.getValue());
                        info.setPriceModifier(option.getPriceModifier());
                    } else {
                        // Fallback to stored value
                        info.setOptionValue(selection.getOptionValue() != null ? selection.getOptionValue() : "Unknown");
                        info.setPriceModifier(selection.getPriceModifier());
                    }
                } else {
                    // Fallback to stored data
                    info.setVariantName(selection.getVariantName() != null ? selection.getVariantName() : "Unknown");
                    info.setVariantType(selection.getVariantType());
                    info.setVariantUnit(selection.getVariantUnit());
                    info.setOptionValue(selection.getOptionValue() != null ? selection.getOptionValue() : "Unknown");
                    info.setPriceModifier(selection.getPriceModifier());
                }
                
                displayList.add(info);
            }
        } catch (Exception e) {
            // If resolution fails, use stored data
            for (VariantSelectionDto selection : variantSelections.values()) {
                if (selection != null) {
                    VariantDisplayInfo info = new VariantDisplayInfo();
                    info.setVariantName(selection.getVariantName() != null ? selection.getVariantName() : "Unknown");
                    info.setVariantType(selection.getVariantType());
                    info.setVariantUnit(selection.getVariantUnit());
                    info.setOptionValue(selection.getOptionValue() != null ? selection.getOptionValue() : "Unknown");
                    info.setPriceModifier(selection.getPriceModifier());
                    displayList.add(info);
                }
            }
        }
        
        return displayList;
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
                EmailTemplateData.OrderItemData.OrderItemDataBuilder builder = EmailTemplateData.OrderItemData.builder()
                    .productName(item.getName())
                    .productImage(item.getImage())
                    .quantity(item.getQuantity())
                    .unitPrice(item.getPrice())
                    .totalPrice(item.getTotalPrice())
                    .productType(item.getProductType());
                
                // Add ZIP password and download URL for digital products
                if ("DIGITAL".equals(item.getProductType())) {
                    builder.zipPassword(item.getZipPassword());
                    builder.digitalDownloadUrl(item.getDigitalDownloadUrl());
                }
                
                orderItems.add(builder.build());
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
    
    @Transactional
    public OrderDto updatePaymentStatusByOrderNumber(String orderNumber, String paymentStatus, String paymentId) {
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderNumber));
        return updatePaymentStatus(order.getId(), paymentStatus, paymentId);
    }
    
    /**
     * Check if invoice exists in Swipe for this order
     * Returns status indicating if invoice was created
     */
    public Map<String, Object> checkSwipeInvoiceStatus(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        
        Map<String, Object> result = new HashMap<>();
        
        // Check our database first
        boolean hasInvoiceInDb = (order.getInvoiceStatus() != null && 
            order.getInvoiceStatus() == Order.InvoiceStatus.CREATED) ||
            (order.getSwipeInvoiceId() != null && !order.getSwipeInvoiceId().trim().isEmpty());
        
        result.put("hasInvoiceInDb", hasInvoiceInDb);
        result.put("invoiceStatus", order.getInvoiceStatus() != null ? order.getInvoiceStatus().name() : "NOT_CREATED");
        result.put("swipeInvoiceId", order.getSwipeInvoiceId());
        result.put("swipeInvoiceNumber", order.getSwipeInvoiceNumber());
        
        // If we have hash_id, try to verify with Swipe
        if (order.getSwipeInvoiceId() != null && !order.getSwipeInvoiceId().trim().isEmpty()) {
            try {
                com.sara.ecom.dto.SwipeDto.SwipeInvoiceResponse swipeResponse = 
                    swipeService.getInvoiceDetails(order.getSwipeInvoiceId());
                if (swipeResponse != null && swipeResponse.getSuccess() != null && swipeResponse.getSuccess()) {
                    result.put("existsInSwipe", true);
                    result.put("swipeInvoiceData", swipeResponse.getData());
                } else {
                    result.put("existsInSwipe", false);
                }
            } catch (Exception e) {
                // If we can't verify with Swipe, assume it exists if we have hash_id
                result.put("existsInSwipe", true);
                result.put("swipeCheckError", e.getMessage());
            }
        } else {
            result.put("existsInSwipe", false);
        }
        
        // Final verdict: invoice exists if either in DB or Swipe
        boolean invoiceExists = hasInvoiceInDb || (Boolean) result.getOrDefault("existsInSwipe", false);
        result.put("invoiceExists", invoiceExists);
        
        return result;
    }
}
