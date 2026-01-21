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
import com.sara.ecom.entity.BusinessConfig;
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
    private BusinessConfigService businessConfigService;
    
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
        
        // Check BusinessConfig for partial COD settings
        BusinessConfig businessConfig = businessConfigService.getConfigEntity();
        
        // For digital products, always use full online payment (ignore COD settings)
        if (!hasDigitalProducts && businessConfig.getPartialCodEnabled() != null && businessConfig.getPartialCodEnabled()) {
            // Partial COD: Calculate advance payment
            Integer advancePercentage = businessConfig.getPartialCodAdvancePercentage();
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
        return updateOrderStatus(orderId, status, null, null, false);
    }
    
    public OrderDto updateOrderStatus(Long orderId, String status, boolean skipWhatsApp) {
        return updateOrderStatus(orderId, status, null, null, skipWhatsApp);
    }
    
    public OrderDto updateOrderStatus(Long orderId, String status, String customStatus, String customMessage, boolean skipWhatsApp) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        
        Order.OrderStatus oldStatus = order.getStatus();
        String oldStatusString = oldStatus != null ? oldStatus.name() : null;
        Order.OrderStatus newStatus = Order.OrderStatus.valueOf(status.toUpperCase());
        order.setStatus(newStatus);
        
        // Set custom status if provided
        if (customStatus != null && !customStatus.trim().isEmpty()) {
            order.setCustomStatus(customStatus.trim());
        } else {
            // Clear custom status when setting standard status
            order.setCustomStatus(null);
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
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        
        String oldStatusString = order.getStatus() != null ? order.getStatus().name() : null;
        
        order.setCustomStatus(customStatus != null ? customStatus.trim() : null);
        
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
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        Order.PaymentStatus oldPaymentStatus = order.getPaymentStatus();
        order.setPaymentStatus(Order.PaymentStatus.valueOf(paymentStatus.toUpperCase()));
        if (paymentId != null) {
            order.setPaymentId(paymentId);
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
    private void generateDigitalProductZip(Order order) {
        if (order.getItems() == null || order.getItems().isEmpty()) {
            return;
        }
        
        // Get user email and phone for password generation
        String userEmail = order.getUserEmail();
        User user = userRepository.findByEmail(userEmail).orElse(null);
        if (user == null) {
            System.err.println("User not found for order: " + order.getId());
            return;
        }
        
        String phoneNumber = user.getPhoneNumber();
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            // Try to get phone from shipping address
            try {
                if (order.getShippingAddress() != null) {
                    Map<String, Object> shippingAddr = objectMapper.readValue(order.getShippingAddress(), new TypeReference<Map<String, Object>>() {});
                    phoneNumber = (String) shippingAddr.get("phone");
                }
            } catch (Exception e) {
                System.err.println("Failed to parse shipping address for phone: " + e.getMessage());
            }
        }
        
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            System.err.println("Phone number not available for password generation in order: " + order.getId());
            return;
        }
        
        // Generate password: First 4 letters of email username (uppercase) + Last 4 digits of mobile
        String emailUsername = userEmail.split("@")[0].toLowerCase();
        String first4Letters = emailUsername.length() >= 4 
            ? emailUsername.substring(0, 4).toUpperCase() 
            : emailUsername.toUpperCase();
        
        String phoneDigits = phoneNumber.replaceAll("\\D", ""); // Remove non-digits
        String last4Digits = phoneDigits.length() >= 4 
            ? phoneDigits.substring(phoneDigits.length() - 4) 
            : phoneDigits;
        
        String zipPassword = first4Letters + last4Digits;
        
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
    public OrderDto updateOrderShippingAddressAdmin(Long orderId, Map<String, Object> shippingAddress) {
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

        try {
            order.setShippingAddress(objectMapper.writeValueAsString(normalized));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to save shipping address", e);
        }

        // Also update userName on order if name is provided (helps admin UI)
        String firstName = (String) normalized.get("firstName");
        String lastName = (String) normalized.get("lastName");
        if (firstName != null && !firstName.trim().isEmpty()) {
            String fullName = firstName.trim() + (lastName != null && !lastName.trim().isEmpty() ? " " + lastName.trim() : "");
            order.setUserName(fullName.trim());
        }

        Order saved = orderRepository.save(order);
        return toOrderDto(saved);
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
        dto.setNotes(order.getNotes());
        dto.setSwipeInvoiceId(order.getSwipeInvoiceId());
        dto.setSwipeInvoiceNumber(order.getSwipeInvoiceNumber());
        dto.setSwipeIrn(order.getSwipeIrn());
        dto.setSwipeQrCode(order.getSwipeQrCode());
        dto.setSwipeInvoiceUrl(order.getSwipeInvoiceUrl());
        dto.setInvoiceStatus(order.getInvoiceStatus() != null ? order.getInvoiceStatus().name() : "NOT_CREATED");
        dto.setInvoiceCreatedAt(order.getInvoiceCreatedAt());
        dto.setCreatedAt(order.getCreatedAt());
        // Payment currency and amount as recorded from gateway (fallbacks handled on frontend)
        dto.setPaymentCurrency(order.getPaymentCurrency());
        dto.setPaymentAmount(order.getPaymentAmount());
        
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
