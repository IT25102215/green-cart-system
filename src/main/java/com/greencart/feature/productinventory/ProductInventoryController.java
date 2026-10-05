package com.greencart.feature.productinventory;

import com.greencart.entity.Category;
import com.greencart.entity.Product;
import com.greencart.service.AuditService;
import com.greencart.service.CategoryService;
import com.greencart.service.FileStorageService;
import com.greencart.service.ProductService;
import com.greencart.util.InputValidation;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class ProductInventoryController {

    private final ProductService productService;
    private final CategoryService categoryService;
    private final FileStorageService storage;
    private final AuditService auditService;

    @Value("${app.inventory.low-stock-threshold:10}")
    private int lowStockThreshold;

    @GetMapping("/products")
    public String products(@RequestParam(required = false) String q,
                           @RequestParam(required = false) Long category,
                           @RequestParam(defaultValue = "ALL") String stock,
                           @RequestParam(defaultValue = "0") int page,
                           Model model) {
        if (!model.containsAttribute("product")) {
            model.addAttribute("product", new Product());
        }
        Page<Product> productPage = productService.searchAdmin(q, category, stock, lowStockThreshold, page, 10);
        model.addAttribute("products", productPage.getContent());
        model.addAttribute("productPage", productPage);
        model.addAttribute("page", productPage.getNumber());
        model.addAttribute("totalPages", productPage.getTotalPages());
        model.addAttribute("q", q);
        model.addAttribute("selectedCategory", category);
        model.addAttribute("stockFilter", stock == null ? "ALL" : stock.toUpperCase());
        model.addAttribute("categories", categoryService.all());
        model.addAttribute("lowStockThreshold", lowStockThreshold);
        return "admin/products";
    }

    @GetMapping("/inventory/report")
    public String inventoryReport(Model model) {
        var lowStock = productService.lowStock(lowStockThreshold);
        var outOfStock = productService.outOfStock();
        model.addAttribute("lowStockThreshold", lowStockThreshold);
        model.addAttribute("totalProducts", productService.count());
        model.addAttribute("totalUnits", productService.totalUnits());
        model.addAttribute("inventoryValue", productService.inventoryValue());
        model.addAttribute("lowStock", lowStock);
        model.addAttribute("outOfStock", outOfStock);
        model.addAttribute("lowStockCount", lowStock.size());
        model.addAttribute("outOfStockCount", outOfStock.size());
        model.addAttribute("categorySummaries", productService.categorySummaries());
        model.addAttribute("inventoryAuditLogs", auditService.recentByModule("INVENTORY"));
        return "admin/inventory-report";
    }

    @PostMapping("/products/save")
    public String saveProduct(@ModelAttribute Product product,
                              @RequestParam(value = "categoryId", required = false) Long categoryId,
                              @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
                              Authentication authentication,
                              RedirectAttributes redirectAttributes) {
        try {
            String cleanName = InputValidation.requireText(product.getName(), "Product name", 2, 150);
            if (product.getPrice() == null || product.getPrice().signum() < 0) {
                throw new IllegalArgumentException("A valid product price is required");
            }
            if (product.getStock() != null && product.getStock() < 0) {
                throw new IllegalArgumentException("Stock cannot be negative");
            }
            if (product.getDiscount() != null
                    && (product.getDiscount().compareTo(BigDecimal.ZERO) < 0
                    || product.getDiscount().compareTo(new BigDecimal("100")) > 0)) {
                throw new IllegalArgumentException("Discount must be between 0 and 100");
            }

            Product existing = product.getId() == null ? null : productService.get(product.getId());
            productService.findByName(cleanName).ifPresent(duplicate -> {
                if (product.getId() == null || !duplicate.getId().equals(product.getId())) {
                    throw new IllegalArgumentException("A product with this name already exists");
                }
            });

            if (categoryId != null) {
                product.setCategory(categoryService.get(categoryId));
            } else {
                product.setCategory(null);
            }

            if (imageFile != null && !imageFile.isEmpty()) {
                product.setImage(storage.store(imageFile));
            } else if (product.getImage() != null && !product.getImage().isBlank()) {
                product.setImage(product.getImage().trim());
            } else if (existing != null) {
                product.setImage(existing.getImage());
            }

            if (product.getStock() == null) product.setStock(0);
            if (product.getDiscount() == null) product.setDiscount(BigDecimal.ZERO);
            product.setName(cleanName);
            if (product.getDescription() != null && !product.getDescription().isBlank()) {
                product.setDescription(InputValidation.requireText(product.getDescription(), "Description", 1, 2000));
            }

            Product saved = productService.save(product, authentication == null ? "SYSTEM" : authentication.getName());
            if (existing == null) {
                auditService.log("INVENTORY", "CREATE", "Product", saved.getId(),
                        "Created product '" + saved.getName() + "' with stock " + saved.getStock(), authentication);
            } else {
                auditService.log("INVENTORY", "UPDATE", "Product", saved.getId(),
                        "Updated product '" + saved.getName() + "'", authentication);
            }
            redirectAttributes.addFlashAttribute("success", "Product saved successfully");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("error", "Error saving product: " + safeMessage(ex));
        }
        return "redirect:/admin/products";
    }

    @GetMapping("/products/edit/{id}")
    public String editProduct(@PathVariable Long id, Model model) {
        Page<Product> productPage = productService.searchAdmin(null, null, "ALL", lowStockThreshold, 0, 10);
        model.addAttribute("products", productPage.getContent());
        model.addAttribute("productPage", productPage);
        model.addAttribute("page", 0);
        model.addAttribute("totalPages", productPage.getTotalPages());
        model.addAttribute("q", null);
        model.addAttribute("selectedCategory", null);
        model.addAttribute("stockFilter", "ALL");
        model.addAttribute("product", productService.get(id));
        model.addAttribute("categories", categoryService.all());
        model.addAttribute("lowStockThreshold", lowStockThreshold);
        return "admin/products";
    }

    @PostMapping("/products/delete/{id}")
    public String deleteProduct(@PathVariable Long id,
                                Authentication authentication,
                                RedirectAttributes redirectAttributes) {
        try {
            Product existing = productService.get(id);
            String description = "Soft-deleted product '" + existing.getName() + "' (last stock " + existing.getStock() + ")";
            productService.delete(id);
            auditService.log("INVENTORY", "DELETE", "Product", id, description, authentication);
            redirectAttributes.addFlashAttribute("success", "Product archived successfully. Historical order data is preserved.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("error", "Delete failed: " + safeMessage(ex));
        }
        return "redirect:/admin/products";
    }

    @PostMapping("/categories/save")
    public String saveCategory(@RequestParam(required = false) Long id,
                               @RequestParam String name,
                               Authentication authentication,
                               RedirectAttributes redirectAttributes) {
        try {
            String cleanName = InputValidation.requireText(name, "Category name", 2, 100);
            var duplicate = categoryService.findByName(cleanName);
            if (duplicate.isPresent() && (id == null || !duplicate.get().getId().equals(id))) {
                throw new IllegalArgumentException("A category with this name already exists");
            }

            Category category = id == null ? new Category() : categoryService.get(id);
            boolean isNew = id == null;
            category.setName(cleanName);
            Category saved = categoryService.save(category);
            auditService.log("INVENTORY", isNew ? "CREATE_CATEGORY" : "UPDATE_CATEGORY", "Category", saved.getId(),
                    (isNew ? "Created" : "Updated") + " category '" + saved.getName() + "'", authentication);

            redirectAttributes.addFlashAttribute("success",
                    isNew ? "Category added successfully." : "Category updated successfully.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("error", "Could not save category: " + safeMessage(ex));
        }
        return "redirect:/admin/products";
    }

    @PostMapping("/categories/delete/{id}")
    public String deleteCategory(@PathVariable Long id,
                                 Authentication authentication,
                                 RedirectAttributes redirectAttributes) {
        try {
            long productCount = productService.countByCategory(id);
            if (productCount > 0) {
                throw new IllegalArgumentException(
                        "This category is used by " + productCount + " product(s). Move or delete those products first."
                );
            }
            Category category = categoryService.get(id);
            String categoryName = category.getName();
            categoryService.delete(id);
            auditService.log("INVENTORY", "DELETE_CATEGORY", "Category", id,
                    "Deleted category '" + categoryName + "'", authentication);
            redirectAttributes.addFlashAttribute("success", "Category deleted successfully.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("error", "Could not delete category: " + safeMessage(ex));
        }
        return "redirect:/admin/products";
    }

    private String safeMessage(Exception ex) {
        return ex.getMessage() == null || ex.getMessage().isBlank()
                ? ex.getClass().getSimpleName()
                : ex.getMessage();
    }
}
