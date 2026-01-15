package com.sara.ecom.repository;

import com.sara.ecom.entity.CustomFormField;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CustomFormFieldRepository extends JpaRepository<CustomFormField, Long> {
    
    List<CustomFormField> findAllByOrderByDisplayOrderAsc();
}
