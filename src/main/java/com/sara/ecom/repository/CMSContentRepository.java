package com.sara.ecom.repository;

import com.sara.ecom.entity.CMSContent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CMSContentRepository extends JpaRepository<CMSContent, String> {
    
    Optional<CMSContent> findByKey(String key);
    
    List<CMSContent> findByKeyStartingWith(String prefix);
}
