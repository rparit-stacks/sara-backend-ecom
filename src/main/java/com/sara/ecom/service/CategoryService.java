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
        return getAllCategories(null);
    }
    
    @Transactional
    public List<CategoryDto> getAllCategories(String userEmail) {
        return getAllCategories(userEmail, false);
    }
    
    @Transactional
    public List<CategoryDto> getAllCategories(String userEmail, boolean isAdmin) {
        List<Category> categories = categoryRepository.findByParentIdIsNull();
        
        // Admins can see all categories
        if (!isAdmin) {
            // Filter by user email if provided
            if (userEmail != null && !userEmail.trim().isEmpty()) {
                categories = categories.stream()
                        .filter(category -> isCategoryAccessible(category, userEmail, false))
                        .collect(Collectors.toList());
            } else {
                // If no user email (not logged in), only show public categories
                categories = categories.stream()
                        .filter(category -> category.getAllowedEmails() == null || category.getAllowedEmails().trim().isEmpty())
                        .collect(Collectors.toList());
            }
        }
        
        return categories.stream()
                .map(this::buildCategoryTree)
                .map(category -> filterCategoryTreeByEmail(category, userEmail, isAdmin))
                .filter(category -> category != null)
                .map(CategoryDto::fromEntity)
                .collect(Collectors.toList());
    }
    
    @Transactional
    public List<CategoryDto> getActiveCategories() {
        return getActiveCategories(null);
    }
    
    @Transactional
    public List<CategoryDto> getActiveCategories(String userEmail) {
        return getActiveCategories(userEmail, false);
    }
    
    @Transactional
    public List<CategoryDto> getActiveCategories(String userEmail, boolean isAdmin) {
        List<Category> categories = categoryRepository.findByParentIdIsNullAndStatus(Category.Status.ACTIVE);
        
        // Admins can see all categories
        if (!isAdmin) {
            // Filter by user email if provided
            if (userEmail != null && !userEmail.trim().isEmpty()) {
                categories = categories.stream()
                        .filter(category -> isCategoryAccessible(category, userEmail, false))
                        .collect(Collectors.toList());
            } else {
                // If no user email (not logged in), only show public categories
                categories = categories.stream()
                        .filter(category -> category.getAllowedEmails() == null || category.getAllowedEmails().trim().isEmpty())
                        .collect(Collectors.toList());
            }
        }
        
        return categories.stream()
                .map(this::buildCategoryTree)
                .map(category -> filterCategoryTreeByEmail(category, userEmail, isAdmin))
                .filter(category -> category != null)
                .map(CategoryDto::fromEntity)
                .collect(Collectors.toList());
    }
    
    /**
     * Gets active categories filtered by user email.
     * Only shows categories that are either:
     * 1. Not restricted (allowedEmails is null or empty)
     * 2. Restricted but user email is in allowedEmails list
     */
    @Transactional
    public List<CategoryDto> getActiveCategoriesForUser(String userEmail) {
        List<Category> allCategories = categoryRepository.findByParentIdIsNullAndStatus(Category.Status.ACTIVE);
        
        // Filter categories based on email restrictions
        List<Category> filteredCategories = allCategories.stream()
                .filter(category -> isCategoryAccessible(category, userEmail))
                .collect(Collectors.toList());
        
        return filteredCategories.stream()
                .map(this::buildCategoryTree)
                .map(category -> filterCategoryTreeByEmail(category, userEmail))
                .filter(category -> category != null) // Remove null categories
                .map(CategoryDto::fromEntity)
                .collect(Collectors.toList());
    }
    
    /**
     * Checks if a category is accessible to a user based on email restrictions.
     * Also checks parent categories recursively - if any parent has restrictions, child inherits them.
     * Admins can always access all categories regardless of restrictions.
     */
    public boolean isCategoryAccessible(Category category, String userEmail) {
        return isCategoryAccessible(category, userEmail, false);
    }
    
    /**
     * Checks if a category is accessible to a user based on email restrictions.
     * Also checks parent categories recursively - if any parent has restrictions, child inherits them.
     * @param isAdmin If true, admin can access all categories regardless of restrictions
     */
    public boolean isCategoryAccessible(Category category, String userEmail, boolean isAdmin) {
        if (category == null) {
            return false;
        }
        
        // Admins can always access all categories
        if (isAdmin) {
            return true;
        }
        
        // Check parent category first (recursively)
        if (category.getParentId() != null) {
            Category parent = categoryRepository.findById(category.getParentId()).orElse(null);
            if (parent != null && !isCategoryAccessible(parent, userEmail, false)) {
                // If parent is not accessible, child is not accessible
                return false;
            }
        }
        
        // Check current category's restrictions
        // If no email restriction, category is accessible (assuming parent is accessible)
        if (category.getAllowedEmails() == null || category.getAllowedEmails().trim().isEmpty()) {
            return true;
        }
        
        // If user email is null, only show unrestricted categories
        if (userEmail == null || userEmail.trim().isEmpty()) {
            return false;
        }
        
        // Check if user email is in the allowed emails list
        String[] allowedEmails = category.getAllowedEmails().split(",");
        for (String email : allowedEmails) {
            if (email.trim().equalsIgnoreCase(userEmail.trim())) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Recursively filters category tree by email, removing inaccessible subcategories.
     */
    private Category filterCategoryTreeByEmail(Category category, String userEmail) {
        return filterCategoryTreeByEmail(category, userEmail, false);
    }
    
    /**
     * Recursively filters category tree by email, removing inaccessible subcategories.
     * @param isAdmin If true, admin can see all categories
     */
    private Category filterCategoryTreeByEmail(Category category, String userEmail, boolean isAdmin) {
        if (category == null) {
            return null;
        }
        
        // Admins can see all categories
        if (isAdmin) {
            // Still need to build subcategories tree
            List<Category> subcategories = categoryRepository.findByParentId(category.getId());
            List<Category> allSubcategories = subcategories.stream()
                    .map(sub -> filterCategoryTreeByEmail(sub, userEmail, true))
                    .filter(sub -> sub != null)
                    .collect(Collectors.toList());
            category.setSubcategories(allSubcategories);
            return category;
        }
        
        // Filter subcategories
        List<Category> subcategories = categoryRepository.findByParentId(category.getId());
        List<Category> accessibleSubcategories = subcategories.stream()
                .filter(sub -> isCategoryAccessible(sub, userEmail, false))
                .map(sub -> filterCategoryTreeByEmail(sub, userEmail, false))
                .filter(sub -> sub != null)
                .collect(Collectors.toList());
        
        category.setSubcategories(accessibleSubcategories);
        
        // If category itself is not accessible, return null
        if (!isCategoryAccessible(category, userEmail, false)) {
            return null;
        }
        
        return category;
    }
    
    /**
     * Recursively filters CategoryDto by email, removing inaccessible subcategories.
     */
    private CategoryDto filterCategoryDtoByEmail(CategoryDto dto, String userEmail) {
        return filterCategoryDtoByEmail(dto, userEmail, false);
    }
    
    /**
     * Recursively filters CategoryDto by email, removing inaccessible subcategories.
     * @param isAdmin If true, admin can see all categories
     */
    private CategoryDto filterCategoryDtoByEmail(CategoryDto dto, String userEmail, boolean isAdmin) {
        if (dto == null) {
            return null;
        }
        
        // Admins can see all categories
        if (isAdmin) {
            // Still need to filter subcategories recursively
            if (dto.getSubcategories() != null && !dto.getSubcategories().isEmpty()) {
                List<CategoryDto> allSubcategories = dto.getSubcategories().stream()
                        .map(sub -> filterCategoryDtoByEmail(sub, userEmail, true))
                        .filter(sub -> sub != null)
                        .collect(Collectors.toList());
                dto.setSubcategories(allSubcategories);
            }
            return dto;
        }
        
        // Check if category itself is accessible
        Category category = categoryRepository.findById(dto.getId()).orElse(null);
        if (category != null && !isCategoryAccessible(category, userEmail, false)) {
            return null;
        }
        
        // Filter subcategories recursively
        if (dto.getSubcategories() != null && !dto.getSubcategories().isEmpty()) {
            List<CategoryDto> accessibleSubcategories = dto.getSubcategories().stream()
                    .map(sub -> filterCategoryDtoByEmail(sub, userEmail, false))
                    .filter(sub -> sub != null)
                    .collect(Collectors.toList());
            dto.setSubcategories(accessibleSubcategories);
        }
        
        return dto;
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
        return getCategoryBySlug(slug, null);
    }
    
    @Transactional
    public CategoryDto getCategoryBySlug(String slug, String userEmail) {
        return getCategoryBySlug(slug, userEmail, false);
    }
    
    @Transactional
    public CategoryDto getCategoryBySlug(String slug, String userEmail, boolean isAdmin) {
        Category category = categoryRepository.findBySlugAndStatus(slug, Category.Status.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("Category", slug));
        
        // Check if category is accessible (admins can access all)
        if (!isCategoryAccessible(category, userEmail, isAdmin)) {
            throw new ResourceNotFoundException("Category", slug);
        }
        
        CategoryDto dto = CategoryDto.fromEntity(buildCategoryTree(category));
        // Filter subcategories by email (admins see all)
        dto = filterCategoryDtoByEmail(dto, userEmail, isAdmin);
        enrichCategoryWithProductCount(dto);
        return dto;
    }
    
    @Transactional
    public CategoryDto getCategoryBySlugPath(String slugPath) {
        return getCategoryBySlugPath(slugPath, null);
    }
    
    @Transactional
    public CategoryDto getCategoryBySlugPath(String slugPath, String userEmail) {
        return getCategoryBySlugPath(slugPath, userEmail, false);
    }
    
    @Transactional
    public CategoryDto getCategoryBySlugPath(String slugPath, String userEmail, boolean isAdmin) {
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
        
        // Check if root category is accessible (admins can access all)
        if (!isCategoryAccessible(currentCategory, userEmail, isAdmin)) {
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
            
            // Check if subcategory is accessible (admins can access all)
            if (!isCategoryAccessible(found, userEmail, isAdmin)) {
                throw new ResourceNotFoundException("Category", slug);
            }
            
            currentCategory = found;
        }
        
        CategoryDto dto = CategoryDto.fromEntity(buildCategoryTree(currentCategory));
        // Filter subcategories by email (admins see all)
        dto = filterCategoryDtoByEmail(dto, userEmail, isAdmin);
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
                .allowedEmails(request.getAllowedEmails())
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
        if (request.getAllowedEmails() != null) {
            category.setAllowedEmails(request.getAllowedEmails());
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
