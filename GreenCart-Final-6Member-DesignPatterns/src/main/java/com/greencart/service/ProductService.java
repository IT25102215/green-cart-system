package com.greencart.service;

import com.greencart.designpattern.member1product.observer.InventoryStockEvent;
import com.greencart.designpattern.member1product.observer.InventorySubject;
import com.greencart.dto.InventoryCategorySummaryDto;
import com.greencart.entity.Product;
import com.greencart.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ProductService {
    private final ProductRepository repo;
    private final InventorySubject inventorySubject;

    public Page<Product> search(String q, Long cid, int page, int size) {
        return repo.search(clean(q), cid, PageRequest.of(Math.max(page, 0), size, Sort.by("id").descending()));
    }

    public Page<Product> searchAdmin(String q, Long cid, String stockMode, int threshold, int page, int size) {
        String mode = stockMode == null || stockMode.isBlank() ? "ALL" : stockMode.trim().toUpperCase(Locale.ROOT);
        if (!Set.of("ALL", "LOW", "OUT", "IN").contains(mode)) mode = "ALL";
        return repo.searchAdmin(clean(q), cid, mode, threshold,
                PageRequest.of(Math.max(page, 0), size, Sort.by("id").descending()));
    }

    public List<Product> featured() { return repo.findTop24ByOrderByIdDesc(); }

    public Page<Product> offers(int page, int size) {
        return repo.findActiveOffers(BigDecimal.ZERO,
                PageRequest.of(Math.max(page, 0), size, Sort.by("id").descending()));
    }

    public Product get(Long id) {
        return repo.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));
    }

    public Optional<Product> findByName(String name) {
        return name == null ? Optional.empty() : repo.findActiveByNameIgnoreCase(name.trim());
    }

    public Product save(Product p) {
        return save(p, "SYSTEM");
    }

    /**
     * Member 1 Observer Pattern integration: ProductService is the business client that
     * changes Product stock; InventorySubject then notifies every InventoryObserver.
     */
    @Transactional
    public Product save(Product p, String actorEmail) {
        Integer oldStock = null;
        if (p.getId() != null) {
            oldStock = repo.findByIdAndDeletedAtIsNull(p.getId())
                    .map(Product::getStock)
                    .orElse(null);
        }

        p.setDeletedAt(null);
        Product saved = repo.saveAndFlush(p);
        if (oldStock == null || !Objects.equals(oldStock, saved.getStock())) {
            inventorySubject.notifyObservers(new InventoryStockEvent(
                    saved, oldStock, saved.getStock(), actorEmail
            ));
        }
        return saved;
    }

    /** Soft delete keeps historical order references intact. */
    @Transactional
    public void delete(Long id) {
        Product product = get(id);
        product.setDeletedAt(LocalDateTime.now());
        // Release the category FK so an unused category can still be removed later.
        product.setCategory(null);
        repo.save(product);
    }

    public long count() { return repo.countByDeletedAtIsNull(); }
    public long countByCategory(Long categoryId) { return repo.countByCategory_IdAndDeletedAtIsNull(categoryId); }
    public List<Product> all() { return repo.findByDeletedAtIsNullOrderByIdDesc(); }
    public List<Product> byCategory(Long categoryId) { return repo.findByCategory_IdAndDeletedAtIsNullOrderByIdDesc(categoryId); }

    public List<Product> lowStock(int threshold) {
        return repo.findByDeletedAtIsNullAndStockLessThanEqualAndStockGreaterThanOrderByStockAsc(threshold, 0);
    }

    public List<Product> outOfStock() {
        return repo.findByDeletedAtIsNullAndStockOrderByNameAsc(0);
    }

    public long totalUnits() {
        return all().stream().mapToLong(p -> p.getStock() == null ? 0 : p.getStock()).sum();
    }

    public BigDecimal inventoryValue() {
        return all().stream()
                .map(p -> p.getFinalPrice().multiply(BigDecimal.valueOf(p.getStock() == null ? 0 : p.getStock())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public List<InventoryCategorySummaryDto> categorySummaries() {
        Map<String, List<Product>> grouped = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (Product product : all()) {
            String category = product.getCategory() == null ? "Uncategorized" : product.getCategory().getName();
            grouped.computeIfAbsent(category, ignored -> new ArrayList<>()).add(product);
        }

        return grouped.entrySet().stream().map(entry -> {
            long units = entry.getValue().stream().mapToLong(p -> p.getStock() == null ? 0 : p.getStock()).sum();
            BigDecimal value = entry.getValue().stream()
                    .map(p -> p.getFinalPrice().multiply(BigDecimal.valueOf(p.getStock() == null ? 0 : p.getStock())))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            return new InventoryCategorySummaryDto(entry.getKey(), entry.getValue().size(), units, value);
        }).toList();
    }

    private String clean(String q) {
        return q == null || q.isBlank() ? null : q.trim();
    }
}
