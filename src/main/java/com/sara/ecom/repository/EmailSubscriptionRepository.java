package com.sara.ecom.repository;

import com.sara.ecom.entity.EmailSubscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EmailSubscriptionRepository extends JpaRepository<EmailSubscription, Long> {
    Optional<EmailSubscription> findByEmail(String email);
    boolean existsByEmail(String email);
}
