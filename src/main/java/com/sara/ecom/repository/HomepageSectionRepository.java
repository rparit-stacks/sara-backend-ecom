package com.sara.ecom.repository;

import com.sara.ecom.entity.HomepageSection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HomepageSectionRepository extends JpaRepository<HomepageSection, Long> {
    
    List<HomepageSection> findBySectionTypeOrderByDisplayOrderAsc(HomepageSection.SectionType sectionType);
    
    void deleteBySectionType(HomepageSection.SectionType sectionType);
}
