package com.sara.ecom.repository;

import com.sara.ecom.entity.OrderStatusTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderStatusTemplateRepository extends JpaRepository<OrderStatusTemplate, Long> {
    Optional<OrderStatusTemplate> findByStatusType(OrderStatusTemplate.StatusType statusType);
    List<OrderStatusTemplate> findAllByOrderByStatusTypeAsc();
}
