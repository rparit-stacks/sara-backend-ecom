package com.sara.ecom.repository;

import com.sara.ecom.entity.CurrencyMultiplier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CurrencyMultiplierRepository extends JpaRepository<CurrencyMultiplier, Long> {

    Optional<CurrencyMultiplier> findByCurrencyCodeIgnoreCase(String currencyCode);
}

