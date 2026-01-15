package com.sara.ecom.service;

import com.sara.ecom.dto.CategoryDto;
import com.sara.ecom.dto.CategoryRequest;
import com.sara.ecom.entity.Category;
import com.sara.ecom.entity.Product;
import com.sara.ecom.exception.ResourceNotFoundException;
import com.sara.ecom.repository.CategoryRepository;
import com.sara.ecom.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryService {
    
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    
    @Transactional
    public List<CategoryDto> getAllCategories() {
        List<Category> categories = categoryRepository.findByParentIdIsNull();
        return categories.stream()
                .map(this::buildCategoryTree)
                .map(CategoryDto::fromEntity)
                .collect(Collectors.toList());
    }
    
    @Transactional
    public List<CategoryDto> getActiveCategories() {
        List<Category> categories = categoryRepository.findByParentIdIsNullAndStatus(Category.Status.ACTIVE);
        return categories.stream()
                .map(this::buildCategoryTree)
                .map(CategoryDto::fromEntity)
                .collect(Collectors.toList());
    }
    
    @Transactional
    public CategoryDto getCategoryById(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", id));
        CategoryDto dto = CategoryDto.fromEntity(buildCategoryTree(category));
        enrichCategoryWithProductCount(dto);
        return dto;
    }
    
    @Transactional
    public CategoryDto getCategoryBySlug(String slug) {
        Category category = categoryRepository.findBySlugAndStatus(slug, Category.Status.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("Category", slug));
        CategoryDto dto = CategoryDto.fromEntity(buildCategoryTree(category));
        enrichCategoryWithProductCount(dto);
        return dto;
    }
    
    @Transactional
    public CategoryDto getCategoryBySlugPath(String slugPath) {
        // Parse hierarchical slug path: "men/shirts/formal-shirts"
        // NOTE: This method supports UNLIMITED depth - no 3-layer limit!
        // It will navigate through as many levels as provided in the path
        String[] slugs = slugPath.split("/");
        if (slugs.length == 0 || slugPath.trim().isEmpty()) {
            throw new ResourceNotFoundException("Category", "empty path");
        }
        
        // Build the full path for error messages
        StringBuilder pathSoFar = new StringBuilder();
        
        // Start with first slug (parent category)
        pathSoFar.append(slugs[0]);
        Category currentCategory = categoryRepository.findBySlugAndStatus(slugs[0], Category.Status.ACTIVE)
                .orElse(null);
        
        // If first level not found with ACTIVE status, check if it exists at all (for better error message)
        if (currentCategory == null) {
            boolean existsButInactive = categoryRepository.findBySlug(slugs[0]).isPresent();
            if (existsButInactive) {
                throw new ResourceNotFoundException("Category '" + slugs[0] + "' exists but is not active");
            }
            throw new ResourceNotFoundException("Category", slugs[0]);
        }
        
        // Navigate through subcategories level by level
        // This loop supports UNLIMITED depth - no maximum limit!
        for (int i = 1; i < slugs.length; i++) {
            String slug = slugs[i].trim(); // Trim whitespace
            if (slug.isEmpty()) {
                continue; // Skip empty slugs
            }
            pathSoFar.append("/").append(slug);
            
            // Get all subcategories (both active and inactive for checking)
            List<Category> allSubcategories = categoryRepository.findByParentId(currentCategory.getId());
            
            // First try to find in active subcategories
            List<Category> activeSubcategories = categoryRepository.findByParentIdAndStatus(
                    currentCategory.getId(), Category.Status.ACTIVE);
            
            Category found = activeSubcategories.stream()
                    .filter(sub -> sub.getSlug() != null && sub.getSlug().equals(slug))
                    .findFirst()
                    .orElse(null);
            
            if (found == null) {
                // Check if subcategory exists but is not active
                boolean existsButInactive = allSubcategories.stream()
                        .anyMatch(sub -> sub.getSlug() != null && sub.getSlug().equals(slug));
                
                if (existsButInactive) {
                    throw new ResourceNotFoundException(
                            "Category '" + slug + "' in path '" + pathSoFar + "' exists but is not active");
                }
                
                // Debug: log available subcategories for troubleshooting
                String availableSlugs = allSubcategories.stream()
                        .map(Category::getSlug)
                        .filter(s -> s != null)
                        .collect(java.util.stream.Collectors.joining(", "));
                
                throw new ResourceNotFoundException(
                        "Category '" + slug + "' not found in path '" + pathSoFar + "'. " +
                        "Available subcategories: " + (availableSlugs.isEmpty() ? "none" : availableSlugs));
            }
            
            currentCategory = found;
        }
        
        CategoryDto dto = CategoryDto.fromEntity(buildCategoryTree(currentCategory));
        enrichCategoryWithProductCount(dto);
        return dto;
    }
    
    /**
     * Enriches CategoryDto with product count information for the category and all its children.
     * This helps frontend distinguish between "category not found" and "category has no products".
     */
    private void enrichCategoryWithProductCount(CategoryDto dto) {
        try {
            // Get all category IDs (current + all descendants)
            List<Long> allCategoryIds = getAllCategoryIds(dto);
            
            // Count products in this category and all child categories
            long totalProductCount = 0;
            if (!allCategoryIds.isEmpty()) {
                totalProductCount = productRepository.countActiveProductsByCategoryIds(
                    allCategoryIds, Product.Status.ACTIVE);
            }
            dto.setProductCount(totalProductCount);
            
            // Also set direct product count (products directly in this category, not in subcategories)
            long directProductCount = 0;
            if (dto.getId() != null) {
                directProductCount = productRepository.countActiveProductsByCategoryId(
                    dto.getId(), Product.Status.ACTIVE);
            }
            dto.setDirectProductCount(directProductCount);
            
            // Recursively enrich subcategories
            if (dto.getSubcategories() != null) {
                for (CategoryDto sub : dto.getSubcategories()) {
                    enrichCategoryWithProductCount(sub);
                }
            }
        } catch (Exception e) {
            // If product count fails, set to 0 and continue (don't break category loading)
            dto.setProductCount(0L);
            dto.setDirectProductCount(0L);
        }
    }
    
    /**
     * Gets all category IDs including the category itself and all its descendants.
     */
    private List<Long> getAllCategoryIds(CategoryDto category) {
        List<Long> ids = new ArrayList<>();
        ids.add(category.getId());
        
        if (category.getSubcategories() != null) {
            for (CategoryDto sub : category.getSubcategories()) {
                ids.addAll(getAllCategoryIds(sub));
            }
        }
        
        return ids;
    }
    
    /**
     * Gets all descendant category IDs for a given category ID.
     */
    @Transactional(readOnly = true)
    public List<Long> getAllDescendantCategoryIds(Long categoryId) {
        List<Long> ids = new ArrayList<>();
        ids.add(categoryId);
        
        List<Category> subcategories = categoryRepository.findByParentId(categoryId);
        for (Category sub : subcategories) {
            ids.addAll(getAllDescendantCategoryIds(sub.getId()));
        }
        
        return ids;
    }
    
    private String generateUniqueSlug(String baseName, Long parentId) {
        String baseSlug = generateSlug(baseName);
        String slug = baseSlug;
        int counter = 1;
        
        // Check if slug exists (slugs must be globally unique)
        while (categoryRepository.existsBySlug(slug)) {
            slug = baseSlug + "-" + counter;
            counter++;
            // Prevent infinite loop
            if (counter > 1000) {
                slug = baseSlug + "-" + System.currentTimeMillis();
                break;
            }
        }
        
        return slug;
    }
    
    private String generateSlug(String name) {
        if (name == null) return "";
        return name.toLowerCase()
                .trim()
                .replaceAll("[^a-z0-9\\s-]", "") // Remove special characters
                .replaceAll("\\s+", "-") // Replace spaces with hyphens
                .replaceAll("-+", "-") // Replace multiple hyphens with single
                .replaceAll("^-|-$", ""); // Remove leading/trailing hyphens
    }
    
    @Transactional
    public CategoryDto createCategory(CategoryRequest request) {
        // Check for duplicate name at same level
        if (request.getParentId() != null) {
            if (categoryRepository.existsByNameAndParentId(request.getName(), request.getParentId())) {
                throw new RuntimeException("Category with this name already exists at this level");
            }
            // Verify parent exists
            categoryRepository.findById(request.getParentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Parent category", request.getParentId()));
        } else {
            if (categoryRepository.existsByNameAndParentId(request.getName(), null)) {
                throw new RuntimeException("Category with this name already exists");
            }
        }
        
        // Generate unique slug
        String slug = generateUniqueSlug(request.getName(), request.getParentId());
        
        Category category = Category.builder()
                .name(request.getName())
                .slug(slug)
                .parentId(request.getParentId())
                .status(request.getStatus() != null ? 
                        Category.Status.valueOf(request.getStatus().toUpperCase()) : 
                        Category.Status.ACTIVE)
                .image(request.getImage())
                .description(request.getDescription())
                .displayOrder(request.getDisplayOrder())
                .isFabric(request.getIsFabric() != null ? request.getIsFabric() : false)
                .build();
        
        category = categoryRepository.save(category);
        return CategoryDto.fromEntity(buildCategoryTree(category));
    }
    
    @Transactional
    public CategoryDto updateCategory(Long id, CategoryRequest request) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", id));
        
        // Check for duplicate name (excluding current category)
        if (request.getParentId() != null) {
            if (categoryRepository.existsByNameAndParentIdAndIdNot(request.getName(), request.getParentId(), id)) {
                throw new RuntimeException("Category with this name already exists at this level");
            }
        } else {
            if (categoryRepository.existsByNameAndParentIdAndIdNot(request.getName(), null, id)) {
                throw new RuntimeException("Category with this name already exists");
            }
        }
        
        category.setName(request.getName());
        
        // Update slug if name changed
        if (!category.getName().equals(request.getName())) {
            String baseSlug = generateSlug(request.getName());
            String newSlug = baseSlug;
            int counter = 1;
            
            // Generate unique slug, excluding current category
            while (categoryRepository.existsBySlugAndIdNot(newSlug, id)) {
                newSlug = baseSlug + "-" + counter;
                counter++;
                if (counter > 1000) {
                    newSlug = baseSlug + "-" + System.currentTimeMillis();
                    break;
                }
            }
            category.setSlug(newSlug);
        }
        
        if (request.getStatus() != null) {
            category.setStatus(Category.Status.valueOf(request.getStatus().toUpperCase()));
        }
        if (request.getImage() != null) {
            category.setImage(request.getImage());
        }
        if (request.getDescription() != null) {
            category.setDescription(request.getDescription());
        }
        if (request.getDisplayOrder() != null) {
            category.setDisplayOrder(request.getDisplayOrder());
        }
        if (request.getIsFabric() != null) {
            category.setIsFabric(request.getIsFabric());
        }
        
        category = categoryRepository.save(category);
        return CategoryDto.fromEntity(buildCategoryTree(category));
    }
    
    @Transactional
    public void deleteCategory(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", id));
        
        // Check if category has subcategories
        List<Category> subcategories = categoryRepository.findByParentId(id);
        if (!subcategories.isEmpty()) {
            throw new RuntimeException("Cannot delete category with subcategories. Please delete subcategories first.");
        }
        
        categoryRepository.delete(category);
    }
    
    @Transactional(readOnly = true)
    public boolean isLeafCategory(Long categoryId) {
        List<Category> subcategories = categoryRepository.findByParentId(categoryId);
        return subcategories.isEmpty();
    }
    
    @Transactional(readOnly = true)
    public List<CategoryDto> getLeafCategories() {
        List<Category> allCategories = categoryRepository.findAll();
        return allCategories.stream()
                .filter(cat -> {
                    List<Category> subcategories = categoryRepository.findByParentId(cat.getId());
                    return subcategories.isEmpty();
                })
                .map(CategoryDto::fromEntity)
                .collect(Collectors.toList());
    }
    
    @Transactional
    private Category buildCategoryTree(Category category) {
        // Ensure slug exists (for existing categories without slugs)
        if (category.getSlug() == null || category.getSlug().isEmpty()) {
            String slug = generateUniqueSlug(category.getName(), category.getParentId());
            category.setSlug(slug);
            categoryRepository.save(category);
        }
        
        List<Category> subcategories = categoryRepository.findByParentId(category.getId());
        category.setSubcategories(subcategories);
        
        // Recursively build tree for subcategories
        for (Category sub : subcategories) {
            buildCategoryTree(sub);
        }
        
        return category;
    }
}
