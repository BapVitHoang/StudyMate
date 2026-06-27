package com.hcmute.studymate.service;

import com.hcmute.studymate.model.Category;
import com.hcmute.studymate.repository.CategoryRepository;
import com.hcmute.studymate.utils.ListCallback;
import com.hcmute.studymate.utils.OperationCallback;

import java.util.Locale;

public class CategoryService {
    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    public void loadCategories(String userId, ListCallback<Category> callback) {
        categoryRepository.getCategories(userId, callback);
    }

    public void createCategory(String userId, String categoryName, OperationCallback callback) {
        if (isBlank(categoryName)) {
            callback.onError(new IllegalArgumentException("Category name is required"));
            return;
        }

        String normalizedName = categoryName.trim();
        String categoryId = buildCategoryId(normalizedName);
        if (isBlank(categoryId)) {
            categoryId = "category_" + System.currentTimeMillis();
        }
        Category category = new Category(categoryId, normalizedName, "#2563EB");
        categoryRepository.saveCategory(userId, category, callback);
    }

    private String buildCategoryId(String categoryName) {
        return categoryName.toLowerCase(Locale.US)
                .replaceAll("[^a-z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
