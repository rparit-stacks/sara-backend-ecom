package com.sara.ecom.service;

import com.sara.ecom.dto.BlogDto;
import com.sara.ecom.dto.BlogRequest;
import com.sara.ecom.entity.Blog;
import com.sara.ecom.repository.BlogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class BlogService {
    
    @Autowired
    private BlogRepository blogRepository;
    
    public List<BlogDto> getAllBlogs(String status, String category) {
        List<Blog> blogs;
        
        if (status != null && category != null) {
            Blog.Status statusEnum = Blog.Status.valueOf(status.toUpperCase());
            blogs = blogRepository.findByCategoryAndStatusOrderByPublishedAtDesc(category, statusEnum);
        } else if (status != null) {
            Blog.Status statusEnum = Blog.Status.valueOf(status.toUpperCase());
            blogs = blogRepository.findByStatusOrderByPublishedAtDesc(statusEnum);
        } else {
            blogs = blogRepository.findAllByOrderByPublishedAtDesc();
        }
        
        return blogs.stream().map(this::toDto).collect(Collectors.toList());
    }
    
    public List<BlogDto> getActiveBlogs(String category) {
        List<Blog> blogs;
        if (category != null) {
            blogs = blogRepository.findByCategoryAndStatusOrderByPublishedAtDesc(category, Blog.Status.ACTIVE);
        } else {
            blogs = blogRepository.findByStatusOrderByPublishedAtDesc(Blog.Status.ACTIVE);
        }
        return blogs.stream().map(this::toDto).collect(Collectors.toList());
    }
    
    @Transactional
    public BlogDto getBlogById(Long id, boolean incrementViews) {
        Blog blog = blogRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Blog not found with id: " + id));
        
        if (incrementViews) {
            blog.incrementViews();
            blogRepository.save(blog);
        }
        
        return toDto(blog);
    }
    
    public List<String> getAllCategories() {
        return blogRepository.findAllCategories();
    }
    
    @Transactional
    public BlogDto createBlog(BlogRequest request) {
        Blog blog = new Blog();
        mapRequestToBlog(request, blog);
        return toDto(blogRepository.save(blog));
    }
    
    @Transactional
    public BlogDto updateBlog(Long id, BlogRequest request) {
        Blog blog = blogRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Blog not found with id: " + id));
        mapRequestToBlog(request, blog);
        return toDto(blogRepository.save(blog));
    }
    
    @Transactional
    public void deleteBlog(Long id) {
        if (!blogRepository.existsById(id)) {
            throw new RuntimeException("Blog not found with id: " + id);
        }
        blogRepository.deleteById(id);
    }
    
    private void mapRequestToBlog(BlogRequest request, Blog blog) {
        blog.setTitle(request.getTitle());
        blog.setExcerpt(request.getExcerpt());
        blog.setContent(request.getContent());
        blog.setImage(request.getImage());
        blog.setAuthor(request.getAuthor() != null ? request.getAuthor() : "Studio Sara");
        blog.setCategory(request.getCategory());
        
        if (request.getStatus() != null) {
            Blog.Status newStatus = Blog.Status.valueOf(request.getStatus().toUpperCase());
            if (blog.getStatus() != Blog.Status.ACTIVE && newStatus == Blog.Status.ACTIVE) {
                blog.setPublishedAt(LocalDateTime.now());
            }
            blog.setStatus(newStatus);
        }
    }
    
    private BlogDto toDto(Blog blog) {
        BlogDto dto = new BlogDto();
        dto.setId(blog.getId());
        dto.setTitle(blog.getTitle());
        dto.setExcerpt(blog.getExcerpt());
        dto.setContent(blog.getContent());
        dto.setImage(blog.getImage());
        dto.setAuthor(blog.getAuthor());
        dto.setCategory(blog.getCategory());
        dto.setStatus(blog.getStatus().name());
        dto.setViews(blog.getViews());
        dto.setPublishedAt(blog.getPublishedAt());
        dto.setReadTime(calculateReadTime(blog.getContent()));
        dto.setIsHomepageFeatured(blog.getIsHomepageFeatured());
        dto.setHomepagePosition(blog.getHomepagePosition());
        return dto;
    }
    
    private String calculateReadTime(String content) {
        if (content == null || content.isEmpty()) {
            return "1 min read";
        }
        int wordCount = content.split("\\s+").length;
        int minutes = Math.max(1, wordCount / 200);
        return minutes + " min read";
    }
    
    @Transactional
    public void setHomepageBlogs(List<Long> blogIds) {
        if (blogIds == null || blogIds.size() != 4) {
            throw new IllegalArgumentException("Exactly 4 blog IDs are required for homepage");
        }
        
        // Clear existing homepage featured flags
        List<Blog> allBlogs = blogRepository.findAll();
        for (Blog blog : allBlogs) {
            blog.setIsHomepageFeatured(false);
            blog.setHomepagePosition(null);
        }
        
        // Set new homepage blogs
        for (int i = 0; i < blogIds.size(); i++) {
            final Long blogId = blogIds.get(i);
            final int position = i + 1;
            Blog blog = blogRepository.findById(blogId)
                    .orElseThrow(() -> new RuntimeException("Blog not found with id: " + blogId));
            blog.setIsHomepageFeatured(true);
            blog.setHomepagePosition(position); // 1-4
            blogRepository.save(blog);
        }
    }
    
    public List<BlogDto> getHomepageBlogs() {
        List<Blog> blogs = blogRepository.findHomepageFeaturedBlogs();
        return blogs.stream().map(this::toDto).collect(Collectors.toList());
    }
}
