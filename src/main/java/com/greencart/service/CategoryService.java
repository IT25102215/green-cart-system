package com.greencart.service;

import com.greencart.entity.Category;
import com.greencart.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CategoryService {
    private final CategoryRepository repo;

    public List<Category> all() {
        return repo.findAll();
    }

    public Category get(Long id) {
        return repo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Category not found"));
    }

    public Optional<Category> findByName(String name) {
        if (name == null || name.isBlank()) {
            return Optional.empty();
        }
        return repo.findByNameIgnoreCase(name.trim());
    }

    public Category save(Category category) {
        return repo.save(category);
    }

    public void delete(Long id) {
        repo.delete(get(id));
    }
}
