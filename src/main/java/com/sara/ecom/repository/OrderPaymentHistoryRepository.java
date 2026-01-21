package com.sara.ecom.repository;

import com.sara.ecom.entity.OrderPaymentHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderPaymentHistoryRepository extends JpaRepository<OrderPaymentHistory, Long> {
    List<OrderPaymentHistory> findByOrderIdOrderByPaidAtDesc(Long orderId);
}
