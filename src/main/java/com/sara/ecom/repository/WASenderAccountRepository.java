package com.sara.ecom.repository;

import com.sara.ecom.entity.WASenderAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WASenderAccountRepository extends JpaRepository<WASenderAccount, Long> {
    Optional<WASenderAccount> findByIsActiveTrue();
    List<WASenderAccount> findAllByOrderByCreatedAtDesc();
    boolean existsByAccountName(String accountName);
}
