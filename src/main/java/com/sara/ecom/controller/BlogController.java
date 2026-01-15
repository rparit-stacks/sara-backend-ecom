package com.sara.ecom.controller;

import com.sara.ecom.dto.BlogDto;
import com.sara.ecom.dto.BlogRequest;
import com.sara.ecom.service.BlogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class BlogController {
    
    @Autowired
    private BlogService blogService;
    
    // Public endpoints
    @GetMapping("/blogs")
    public ResponseEntity<List<BlogDto>> getAllBlogs(@RequestParam(required = false) String category) {
        List<BlogDto> blogs = blogService.getActiveBlogs(category);
        return ResponseEntity.ok(blogs);
    }
    
    @GetMapping("/blogs/{id}")
    public ResponseEntity<BlogDto> getBlogById(@PathVariable Long id) {
        BlogDto blog = blogService.getBlogById(id, true); // increment views
        return ResponseEntity.ok(blog);
    }
    
    @GetMapping("/blogs/categories")
    public ResponseEntity<List<String>> getCategories() {
        return ResponseEntity.ok(blogService.getAllCategories());
    }
    
    // Admin endpoints
    @GetMapping("/admin/blogs")
    public ResponseEntity<List<BlogDto>> getAllBlogsAdmin(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String category) {
        List<BlogDto> blogs = blogService.getAllBlogs(status, category);
        return ResponseEntity.ok(blogs);
    }
    
    @GetMapping("/admin/blogs/{id}")
    public ResponseEntity<BlogDto> getBlogByIdAdmin(@PathVariable Long id) {
        BlogDto blog = blogService.getBlogById(id, false); // don't increment views
        return ResponseEntity.ok(blog);
    }
    
    @PostMapping("/admin/blogs")
    public ResponseEntity<BlogDto> createBlog(@RequestBody BlogRequest request) {
        BlogDto created = blogService.createBlog(request);
        return ResponseEntity.ok(created);
    }
    
    @PutMapping("/admin/blogs/{id}")
    public ResponseEntity<BlogDto> updateBlog(
            @PathVariable Long id,
            @RequestBody BlogRequest request) {
        BlogDto updated = blogService.updateBlog(id, request);
        return ResponseEntity.ok(updated);
    }
    
    @DeleteMapping("/admin/blogs/{id}")
    public ResponseEntity<Void> deleteBlog(@PathVariable Long id) {
        blogService.deleteBlog(id);
        return ResponseEntity.noContent().build();
    }
}
