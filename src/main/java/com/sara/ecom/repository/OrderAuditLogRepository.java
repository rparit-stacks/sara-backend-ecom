package com.sara.ecom.repository;

import com.sara.ecom.entity.OrderAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderAuditLogRepository extends JpaRepository<OrderAuditLog, Long> {
    List<OrderAuditLog> findByOrderIdOrderByCreatedAtDesc(Long orderId);
}
