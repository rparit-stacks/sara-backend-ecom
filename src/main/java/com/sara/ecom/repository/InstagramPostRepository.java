package com.sara.ecom.repository;

import com.sara.ecom.entity.InstagramPost;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InstagramPostRepository extends JpaRepository<InstagramPost, Long> {
    
    List<InstagramPost> findAllByOrderByDisplayOrderAsc();
}
