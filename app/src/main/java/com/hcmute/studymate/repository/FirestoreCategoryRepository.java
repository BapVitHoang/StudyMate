package com.hcmute.studymate.repository;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;
import com.hcmute.studymate.model.Category;
import com.hcmute.studymate.utils.Constants;
import com.hcmute.studymate.utils.ListCallback;
import com.hcmute.studymate.utils.OperationCallback;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class FirestoreCategoryRepository implements CategoryRepository {
    private static final String USERS_COLLECTION = "users";
    private static final String CATEGORIES_COLLECTION = "categories";
    private static final String[] DEFAULT_COLORS = {"#2E7D32", "#1565C0", "#6A1B9A", "#EF6C00"};

    private final FirebaseFirestore firestore;

    public FirestoreCategoryRepository() {
        this(FirebaseFirestore.getInstance());
    }

    FirestoreCategoryRepository(FirebaseFirestore firestore) {
        this.firestore = firestore;
    }

    @Override
    public void getCategories(String userId, ListCallback<Category> callback) {
        if (isBlank(userId)) {
            callback.onError(new IllegalArgumentException("User id is required"));
            return;
        }

        CollectionReference categoriesRef = categoriesRef(userId);
        categoriesRef.get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot.isEmpty()) {
                        seedDefaultCategories(categoriesRef, callback);
                        return;
                    }

                    List<Category> categories = new ArrayList<>();
                    for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                        Category category = document.toObject(Category.class);
                        if (category != null) {
                            if (isBlank(category.getId())) {
                                category.setId(document.getId());
                            }
                            categories.add(category);
                        }
                    }
                    sortByDefaultOrder(categories);
                    callback.onSuccess(categories);
                })
                .addOnFailureListener(callback::onError);
    }

    @Override
    public void saveCategory(String userId, Category category, OperationCallback callback) {
        if (isBlank(userId) || category == null || isBlank(category.getId())) {
            callback.onError(new IllegalArgumentException("User id and category are required"));
            return;
        }

        categoriesRef(userId)
                .document(category.getId())
                .set(category)
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(callback::onError);
    }

    private void seedDefaultCategories(CollectionReference categoriesRef, ListCallback<Category> callback) {
        List<Category> defaults = buildDefaultCategories();
        WriteBatch batch = firestore.batch();
        for (Category category : defaults) {
            batch.set(categoriesRef.document(category.getId()), category);
        }

        batch.commit()
                .addOnSuccessListener(unused -> callback.onSuccess(defaults))
                .addOnFailureListener(callback::onError);
    }

    private List<Category> buildDefaultCategories() {
        List<Category> categories = new ArrayList<>();
        for (int index = 0; index < Constants.DEFAULT_CATEGORY_NAMES.size(); index++) {
            String name = Constants.DEFAULT_CATEGORY_NAMES.get(index);
            String id = name.toLowerCase(Locale.US).replace(" ", "_");
            categories.add(new Category(id, name, DEFAULT_COLORS[index % DEFAULT_COLORS.length]));
        }
        return categories;
    }

    private void sortByDefaultOrder(List<Category> categories) {
        categories.sort((first, second) -> {
            int firstIndex = Constants.DEFAULT_CATEGORY_NAMES.indexOf(first.getName());
            int secondIndex = Constants.DEFAULT_CATEGORY_NAMES.indexOf(second.getName());
            if (firstIndex == -1) {
                firstIndex = Integer.MAX_VALUE;
            }
            if (secondIndex == -1) {
                secondIndex = Integer.MAX_VALUE;
            }
            if (firstIndex == secondIndex) {
                return first.getName().compareToIgnoreCase(second.getName());
            }
            return Integer.compare(firstIndex, secondIndex);
        });
    }

    private CollectionReference categoriesRef(String userId) {
        return firestore.collection(USERS_COLLECTION)
                .document(userId)
                .collection(CATEGORIES_COLLECTION);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
