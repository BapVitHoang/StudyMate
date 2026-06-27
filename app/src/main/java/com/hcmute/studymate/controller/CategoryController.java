package com.hcmute.studymate.controller;

import com.hcmute.studymate.model.Category;
import com.hcmute.studymate.service.CategoryService;
import com.hcmute.studymate.utils.ListCallback;
import com.hcmute.studymate.utils.OperationCallback;

public class CategoryController {
    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    public void loadCategories(String userId, ListCallback<Category> callback) {
        categoryService.loadCategories(userId, callback);
    }

    public void createCategory(String userId, String categoryName, OperationCallback callback) {
        categoryService.createCategory(userId, categoryName, callback);
    }
}
