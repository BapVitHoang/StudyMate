package com.hcmute.studymate.repository;

import com.hcmute.studymate.model.Category;
import com.hcmute.studymate.utils.ListCallback;
import com.hcmute.studymate.utils.OperationCallback;

public interface CategoryRepository {
    void getCategories(String userId, ListCallback<Category> callback);

    void saveCategory(String userId, Category category, OperationCallback callback);
}
