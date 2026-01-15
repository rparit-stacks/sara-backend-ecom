package com.sara.ecom.service;

import com.sara.ecom.dto.DashboardStatsDto;
import com.sara.ecom.entity.Order;
import com.sara.ecom.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class DashboardService {
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private OrderRepository orderRepository;
    
    @Autowired
    private ProductRepository productRepository;
    
    @Autowired
    private CategoryRepository categoryRepository;
    
    public DashboardStatsDto getDashboardStats() {
        DashboardStatsDto stats = new DashboardStatsDto();
        
        // User stats
        stats.setTotalUsers(userRepository.count());
        stats.setActiveUsers(userRepository.countByStatus(com.sara.ecom.entity.User.UserStatus.ACTIVE));
        
        // Order stats
        List<Order> allOrders = orderRepository.findAll();
        stats.setTotalOrders(allOrders.size());
        stats.setPendingOrders(allOrders.stream()
                .filter(o -> o.getStatus() == Order.OrderStatus.PENDING)
                .count());
        
        // Calculate total revenue from completed orders
        BigDecimal revenue = allOrders.stream()
                .filter(o -> o.getPaymentStatus() == Order.PaymentStatus.PAID)
                .map(Order::getTotal)
                .filter(t -> t != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        stats.setTotalRevenue(revenue);
        
        // Product and category counts
        stats.setTotalProducts(productRepository.count());
        stats.setTotalCategories(categoryRepository.count());
        
        // Recent orders (last 10)
        List<Order> recentOrders = orderRepository.findAllByOrderByCreatedAtDesc();
        List<DashboardStatsDto.RecentOrderDto> recentOrderDtos = recentOrders.stream()
                .limit(10)
                .map(this::toRecentOrderDto)
                .collect(Collectors.toList());
        stats.setRecentOrders(recentOrderDtos);
        
        return stats;
    }
    
    private DashboardStatsDto.RecentOrderDto toRecentOrderDto(Order order) {
        DashboardStatsDto.RecentOrderDto dto = new DashboardStatsDto.RecentOrderDto();
        dto.setId(order.getId());
        dto.setOrderNumber(order.getOrderNumber());
        dto.setUserEmail(order.getUserEmail());
        dto.setUserName(order.getUserName());
        dto.setTotal(order.getTotal());
        dto.setStatus(order.getStatus().name());
        if (order.getCreatedAt() != null) {
            dto.setCreatedAt(order.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        }
        return dto;
    }
}
