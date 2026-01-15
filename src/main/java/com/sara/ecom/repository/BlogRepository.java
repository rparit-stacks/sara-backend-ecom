package com.sara.ecom.repository;

import com.sara.ecom.entity.Blog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BlogRepository extends JpaRepository<Blog, Long> {
    
    List<Blog> findByStatusOrderByPublishedAtDesc(Blog.Status status);
    
    List<Blog> findByCategoryAndStatusOrderByPublishedAtDesc(String category, Blog.Status status);
    
    List<Blog> findAllByOrderByPublishedAtDesc();
    
    @Query("SELECT DISTINCT b.category FROM Blog b WHERE b.category IS NOT NULL")
    List<String> findAllCategories();
    
    @Query("SELECT b FROM Blog b WHERE b.status = 'ACTIVE' ORDER BY b.views DESC")
    List<Blog> findPopularBlogs();
}
