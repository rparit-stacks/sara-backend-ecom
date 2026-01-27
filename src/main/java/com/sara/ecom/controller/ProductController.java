package com.sara.ecom.controller;

import com.sara.ecom.dto.ProductDto;
import com.sara.ecom.dto.ProductRequest;
import com.sara.ecom.entity.Product;
import com.sara.ecom.service.ProductService;
import com.sara.ecom.service.JwtService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ProductController {
    
    private static final Logger log = LoggerFactory.getLogger(ProductController.class);
    
    @Autowired
    private ProductService productService;
    
    @Autowired
    private JwtService jwtService;
    
    /**
     * Helper method to check if request is from admin
     */
    private boolean isAdminRequest(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            try {
                String token = authHeader.substring(7);
                return jwtService.isAdminToken(token);
            } catch (Exception e) {
                return false;
            }
        }
        // Also check if path is admin endpoint
        return request.getRequestURI().startsWith("/api/admin/");
    }
    
    // Public endpoints
    @GetMapping("/products")
    public ResponseEntity<List<ProductDto>> getAllProducts(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String userEmail,
            HttpServletRequest request) {
        boolean isAdmin = isAdminRequest(request);
        List<ProductDto> products = productService.getAllProducts(status, type, categoryId, userEmail, isAdmin);
        return ResponseEntity.ok(products);
    }
    
    @GetMapping("/products/{id}")
    public ResponseEntity<ProductDto> getProductById(
            @PathVariable Long id,
            @RequestParam(required = false) String userEmail,
            HttpServletRequest request) {
        boolean isAdmin = isAdminRequest(request);
        ProductDto product = productService.getProductById(id, userEmail, isAdmin);
        return ResponseEntity.ok(product);
    }
    
    // Slug-based product route - must be after /products/{id} to avoid conflicts
    @GetMapping("/products/slug/{slug}")
    public ResponseEntity<ProductDto> getProductBySlug(
            @PathVariable String slug,
            @RequestParam(required = false) String userEmail,
            HttpServletRequest request) {
        boolean isAdmin = isAdminRequest(request);
        ProductDto product = productService.getProductBySlug(slug, userEmail, isAdmin);
        return ResponseEntity.ok(product);
    }
    
    @PostMapping("/products/batch")
    public ResponseEntity<List<ProductDto>> getProductsByIds(@RequestBody Map<String, List<Long>> request) {
        List<Long> ids = request.get("ids");
        List<ProductDto> products = productService.getProductsByIds(ids);
        return ResponseEntity.ok(products);
    }
    
    /**
     * Gets all active products for a category and all its child categories.
     * This is useful for category pages that should show products from subcategories too.
     */
    @GetMapping("/products/category/{categoryId}/with-children")
    public ResponseEntity<List<ProductDto>> getProductsByCategoryWithChildren(
            @PathVariable Long categoryId,
            @RequestParam(required = false) String userEmail,
            HttpServletRequest request) {
        boolean isAdmin = isAdminRequest(request);
        List<ProductDto> products = productService.getProductsByCategoryWithChildren(categoryId, userEmail, isAdmin);
        return ResponseEntity.ok(products);
    }
    
    // Admin endpoints
    @PostMapping("/admin/products")
    public ResponseEntity<ProductDto> createProduct(@RequestBody ProductRequest request) {
        ProductDto created = productService.createProduct(request);
        return ResponseEntity.ok(created);
    }
    
    @PutMapping("/admin/products/{id}")
    public ResponseEntity<ProductDto> updateProduct(
            @PathVariable Long id,
            @RequestBody ProductRequest request) {
        ProductDto updated = productService.updateProduct(id, request);
        return ResponseEntity.ok(updated);
    }
    
    @DeleteMapping("/admin/products/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }
    
    /**
     * Bulk delete products.
     */
    @DeleteMapping("/admin/products/bulk")
    public ResponseEntity<Map<String, Object>> bulkDeleteProducts(@RequestBody Map<String, List<Long>> request) {
        List<Long> ids = request.get("ids");
        if (ids == null || ids.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Product IDs are required"));
        }
        productService.bulkDeleteProducts(ids);
        return ResponseEntity.ok(Map.of("message", "Products deleted successfully", "count", ids.size()));
    }
    
    /**
     * Toggle product status (pause/unpause).
     */
    @PostMapping("/admin/products/{id}/toggle-status")
    public ResponseEntity<ProductDto> toggleProductStatus(@PathVariable Long id) {
        ProductDto updated = productService.toggleProductStatus(id);
        return ResponseEntity.ok(updated);
    }
    
    /**
     * Bulk toggle product status (pause/unpause).
     */
    @PostMapping("/admin/products/bulk/toggle-status")
    public ResponseEntity<Map<String, Object>> bulkToggleProductStatus(@RequestBody Map<String, Object> request) {
        @SuppressWarnings("unchecked")
        List<Long> ids = (List<Long>) request.get("ids");
        String action = (String) request.get("action"); // "pause" or "unpause"
        
        if (ids == null || ids.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Product IDs are required"));
        }
        if (action == null || (!action.equals("pause") && !action.equals("unpause"))) {
            return ResponseEntity.badRequest().body(Map.of("error", "Action must be 'pause' or 'unpause'"));
        }
        
        Product.Status targetStatus = action.equals("pause") ? Product.Status.INACTIVE : Product.Status.ACTIVE;
        productService.bulkToggleProductStatus(ids, targetStatus);
        
        return ResponseEntity.ok(Map.of("message", "Products " + action + "d successfully", "count", ids.size()));
    }
    
    /**
     * Export products to Excel.
     */
    @GetMapping("/admin/products/export")
    public ResponseEntity<org.springframework.core.io.Resource> exportProductsToExcel() {
        try {
            return productService.exportProductsToExcel();
        } catch (Exception e) {
            log.error("Product export failed: {}", e.getMessage(), e);
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * Bulk copy products. Each copy gets name with "-copy" (or "-copy-2", etc.) and a new slug.
     */
    @PostMapping("/admin/products/bulk/copy")
    public ResponseEntity<Map<String, Object>> bulkCopyProducts(@RequestBody Map<String, Object> request) {
        @SuppressWarnings("unchecked")
        List<Number> idsRaw = (List<Number>) request.get("ids");
        if (idsRaw == null || idsRaw.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Product IDs are required"));
        }
        List<Long> ids = idsRaw.stream().map(Number::longValue).toList();
        Map<String, Object> result = productService.copyProducts(ids);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/admin/products/plain")
    public ResponseEntity<ProductDto> createPlainProduct(@RequestBody ProductRequest request) {
        ProductDto created = productService.createPlainProduct(request);
        return ResponseEntity.ok(created);
    }

    @PostMapping("/admin/products/designed")
    public ResponseEntity<ProductDto> createDesignedProduct(@RequestBody ProductRequest request) {
        ProductDto created = productService.createDesignedProduct(request);
        return ResponseEntity.ok(created);
    }

    @PostMapping("/admin/products/digital")
    public ResponseEntity<ProductDto> createDigitalProduct(@RequestBody ProductRequest request) {
        ProductDto created = productService.createDigitalProduct(request);
        return ResponseEntity.ok(created);
    }
    
    /**
     * Creates or gets a Digital Product from a Design Product.
     * This allows users to purchase just the design file without the physical product.
     * Public endpoint - users can access this to purchase designs.
     */
    @PostMapping("/products/{designProductId}/create-digital")
    public ResponseEntity<ProductDto> createDigitalProductFromDesign(
            @PathVariable Long designProductId,
            @RequestBody(required = false) com.sara.ecom.dto.CreateDigitalFromDesignRequest request) {
        java.math.BigDecimal price = request != null ? request.getPrice() : null;
        ProductDto digitalProduct = productService.createDigitalProductFromDesign(designProductId, price);
        return ResponseEntity.ok(digitalProduct);
    }
    
    /**
     * Gets the Digital Product associated with a Design Product (if exists).
     */
    @GetMapping("/products/{designProductId}/digital")
    public ResponseEntity<ProductDto> getDigitalProductFromDesign(@PathVariable Long designProductId) {
        ProductDto digitalProduct = productService.getDigitalProductFromDesign(designProductId);
        if (digitalProduct == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(digitalProduct);
    }
    
    /**
     * Downloads digital product files as a ZIP archive.
     * Fetches all files from Cloudinary URLs and bundles them into a ZIP.
     */
    @GetMapping("/products/{productId}/download-digital")
    public ResponseEntity<org.springframework.core.io.Resource> downloadDigitalProductFiles(@PathVariable Long productId) {
        try {
            return productService.downloadDigitalProductFiles(productId);
        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }
    
    /**
     * DEPRECATED: This endpoint now creates CustomProduct instead of regular Product.
     * Use /api/custom-products endpoint instead.
     * This endpoint is kept for backward compatibility but redirects to CustomProduct creation.
     */
    @PostMapping("/products/create-from-upload")
    public ResponseEntity<Map<String, Object>> createProductFromUpload(
            @RequestBody ProductRequest request,
            org.springframework.security.core.Authentication authentication) {
        // This should now create a CustomProduct, not a regular Product
        // Redirect to CustomProductController
        return ResponseEntity.badRequest().body(Map.of(
            "error", "This endpoint is deprecated. Please use /api/custom-products endpoint instead."
        ));
    }
}
