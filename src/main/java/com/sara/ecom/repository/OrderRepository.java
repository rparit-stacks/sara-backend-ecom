package com.sara.ecom.repository;

import com.sara.ecom.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    
    List<Order> findByUserEmailOrderByCreatedAtDesc(String userEmail);
    
    @Query("SELECT o FROM Order o LEFT JOIN FETCH o.items WHERE o.id = :id")
    Optional<Order> findByIdWithItems(@Param("id") Long id);
    
    @Query("SELECT o FROM Order o LEFT JOIN FETCH o.items WHERE o.id = :id AND o.userEmail = :userEmail")
    Optional<Order> findByIdAndUserEmailWithItems(@Param("id") Long id, @Param("userEmail") String userEmail);
    
    Optional<Order> findByOrderNumber(String orderNumber);
    
    List<Order> findByStatusOrderByCreatedAtDesc(Order.OrderStatus status);
    
    List<Order> findAllByOrderByCreatedAtDesc();
}
