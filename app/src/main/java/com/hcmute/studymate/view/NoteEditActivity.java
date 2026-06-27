package com.hcmute.studymate.view;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.hcmute.studymate.controller.AuthController;
import com.hcmute.studymate.controller.CategoryController;
import com.hcmute.studymate.R;
import com.hcmute.studymate.controller.NoteController;
import com.hcmute.studymate.model.Category;
import com.hcmute.studymate.model.Note;
import com.hcmute.studymate.utils.AppContainer;
import com.hcmute.studymate.utils.Constants;
import com.hcmute.studymate.utils.DataCallback;
import com.hcmute.studymate.utils.ListCallback;
import com.hcmute.studymate.utils.OperationCallback;
import com.hcmute.studymate.utils.TagUtils;

import java.util.ArrayList;
import java.util.List;

public class NoteEditActivity extends AppCompatActivity {
    private static final String EXTRA_NOTE_ID = "extra_note_id";

    private AuthController authController;
    private NoteController noteController;
    private CategoryController categoryController;
    private TextInputLayout titleLayout;
    private TextInputLayout contentLayout;
    private TextInputLayout categoryLayout;
    private TextView screenTitleText;
    private TextInputEditText titleInput;
    private TextInputEditText contentInput;
    private AutoCompleteTextView categoryDropdown;
    private TextInputEditText tagsInput;
    private MaterialButton saveButton;
    private MaterialButton addCategoryButton;
    private ProgressBar categoryLoadingProgress;
    private ArrayAdapter<String> categoryAdapter;
    private final List<String> categoryNames = new ArrayList<>();
    private Note currentNote;
    private String pendingCategorySelection = Constants.CATEGORY_GENERAL;
    private String userId;

    public static Intent newIntent(Context context, String noteId) {
        Intent intent = new Intent(context, NoteEditActivity.class);
        if (noteId != null) {
            intent.putExtra(EXTRA_NOTE_ID, noteId);
        }
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        authController = AppContainer.authController();
        if (!requireLoggedInUser()) {
            return;
        }
        setContentView(R.layout.activity_note_edit);

        noteController = AppContainer.noteController();
        categoryController = AppContainer.categoryController();
        titleLayout = findViewById(R.id.editTitleLayout);
        contentLayout = findViewById(R.id.editContentLayout);
        categoryLayout = findViewById(R.id.editCategoryLayout);
        screenTitleText = findViewById(R.id.editScreenTitleText);
        titleInput = findViewById(R.id.editTitleInput);
        contentInput = findViewById(R.id.editContentInput);
        categoryDropdown = findViewById(R.id.editCategoryDropdown);
        tagsInput = findViewById(R.id.editTagsInput);
        saveButton = findViewById(R.id.saveNoteButton);
        addCategoryButton = findViewById(R.id.addCategoryButton);
        categoryLoadingProgress = findViewById(R.id.categoryLoadingProgress);
        MaterialButton cancelButton = findViewById(R.id.cancelEditButton);

        categoryAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, categoryNames);
        categoryDropdown.setAdapter(categoryAdapter);
        setCategorySelection(Constants.CATEGORY_GENERAL);

        saveButton.setOnClickListener(view -> saveNote());
        addCategoryButton.setOnClickListener(view -> showCreateCategoryDialog());
        cancelButton.setOnClickListener(view -> finish());
        categoryDropdown.setOnItemClickListener((parent, view, position, id) -> updateSaveState(false));
        titleInput.addTextChangedListener(validationWatcher);
        contentInput.addTextChangedListener(validationWatcher);

        String noteId = getIntent().getStringExtra(EXTRA_NOTE_ID);
        if (noteId != null) {
            screenTitleText.setText(R.string.edit_note);
            loadNote(noteId);
        }
        loadCategories();
        updateSaveState(false);
    }

    private void loadNote(String noteId) {
        noteController.loadNote(userId, noteId, new DataCallback<Note>() {
            @Override
            public void onSuccess(Note data) {
                currentNote = data;
                titleInput.setText(data.getTitle());
                contentInput.setText(data.getContent());
                setCategorySelection(data.getCategory());
                tagsInput.setText(TagUtils.join(data.getTags()));
                updateSaveState(false);
            }

            @Override
            public void onError(Exception exception) {
                showError("Could not load this note", exception);
                finish();
            }
        });
    }

    private void loadCategories() {
        setCategoryLoading(true);
        categoryController.loadCategories(userId, new ListCallback<Category>() {
            @Override
            public void onSuccess(List<Category> items) {
                categoryNames.clear();
                for (Category category : items) {
                    if (category.getName() != null && !category.getName().trim().isEmpty()) {
                        categoryNames.add(category.getName());
                    }
                }
                if (categoryNames.isEmpty()) {
                    categoryNames.add(Constants.CATEGORY_GENERAL);
                }
                if (!categoryNames.contains(pendingCategorySelection)) {
                    categoryNames.add(pendingCategorySelection);
                }
                categoryAdapter.notifyDataSetChanged();
                setCategorySelection(pendingCategorySelection);
                setCategoryLoading(false);
            }

            @Override
            public void onError(Exception exception) {
                setCategoryLoading(false);
                showError("Could not load categories", exception);
            }
        });
    }

    private void saveNote() {
        if (!validateInputs(true)) {
            return;
        }

        Note note = currentNote == null ? new Note() : currentNote;
        note.setTitle(readInput(titleInput));
        note.setContent(readInput(contentInput));
        note.setCategory(readCategory());
        note.setTags(TagUtils.parseCsv(readInput(tagsInput)));
        saveButton.setEnabled(false);

        noteController.saveNote(userId, note, new OperationCallback() {
            @Override
            public void onSuccess() {
                Toast.makeText(NoteEditActivity.this, "Note saved", Toast.LENGTH_SHORT).show();
                finish();
            }

            @Override
            public void onError(Exception exception) {
                updateSaveState(false);
                showError("Could not save note", exception);
            }
        });
    }

    private void showCreateCategoryDialog() {
        TextInputLayout inputLayout = new TextInputLayout(this);
        inputLayout.setHint("Category name");
        inputLayout.setBoxBackgroundMode(TextInputLayout.BOX_BACKGROUND_FILLED);
        int padding = getResources().getDimensionPixelSize(R.dimen.space_lg);
        inputLayout.setPadding(padding, 0, padding, 0);
        TextInputEditText input = new TextInputEditText(this);
        inputLayout.addView(input);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Add category")
                .setMessage("Create a subject or class label for future notes.")
                .setView(inputLayout)
                .setPositiveButton("Add", null)
                .setNegativeButton("Cancel", null)
                .create();

        dialog.setOnShowListener(openDialog -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(button -> {
                    String categoryName = readInput(input);
                    if (categoryName.isEmpty()) {
                        inputLayout.setError("Category name is required");
                        return;
                    }
                    inputLayout.setError(null);
                    createCategory(categoryName, dialog);
                }));
        dialog.show();
    }

    private void createCategory(String categoryName, AlertDialog dialog) {
        addCategoryButton.setEnabled(false);
        categoryController.createCategory(userId, categoryName, new OperationCallback() {
            @Override
            public void onSuccess() {
                pendingCategorySelection = categoryName.trim();
                setCategorySelection(pendingCategorySelection);
                loadCategories();
                addCategoryButton.setEnabled(true);
                dialog.dismiss();
                Toast.makeText(NoteEditActivity.this, "Category added", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(Exception exception) {
                addCategoryButton.setEnabled(true);
                showError("Could not add category", exception);
            }
        });
    }

    private void setCategorySelection(String category) {
        pendingCategorySelection = category == null || category.trim().isEmpty()
                ? Constants.CATEGORY_GENERAL
                : category.trim();
        categoryDropdown.setText(pendingCategorySelection, false);
        categoryLayout.setError(null);
    }

    private String readCategory() {
        String category = categoryDropdown.getText() == null ? "" : categoryDropdown.getText().toString().trim();
        return category.isEmpty() ? Constants.CATEGORY_GENERAL : category;
    }

    private void setCategoryLoading(boolean loading) {
        categoryLoadingProgress.setVisibility(loading ? android.view.View.VISIBLE : android.view.View.GONE);
        categoryDropdown.setEnabled(!loading);
        addCategoryButton.setEnabled(!loading);
    }

    private boolean validateInputs(boolean showErrors) {
        boolean hasTitle = !readInput(titleInput).isEmpty();
        boolean hasContent = !readInput(contentInput).isEmpty();
        boolean hasCategory = !readCategory().isEmpty();

        if (showErrors) {
            titleLayout.setError(hasTitle ? null : "Title is required");
            contentLayout.setError(hasContent ? null : "Content is required");
            categoryLayout.setError(hasCategory ? null : "Category is required");
        } else {
            if (hasTitle) {
                titleLayout.setError(null);
            }
            if (hasContent) {
                contentLayout.setError(null);
            }
            if (hasCategory) {
                categoryLayout.setError(null);
            }
        }

        return hasTitle && hasContent && hasCategory;
    }

    private void updateSaveState(boolean showErrors) {
        saveButton.setEnabled(validateInputs(showErrors));
    }

    private String readInput(TextInputEditText input) {
        return input.getText() == null ? "" : input.getText().toString().trim();
    }

    private boolean requireLoggedInUser() {
        userId = authController.getCurrentUserId();
        if (userId != null && !userId.trim().isEmpty()) {
            return true;
        }
        openLoginAndFinish();
        return false;
    }

    private void openLoginAndFinish() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void showError(String prefix, Exception exception) {
        String detail = exception == null || exception.getMessage() == null
                ? "Please check your connection and Firestore rules."
                : exception.getMessage();
        Toast.makeText(this, prefix + ". " + detail, Toast.LENGTH_LONG).show();
    }

    private final TextWatcher validationWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            updateSaveState(false);
        }

        @Override
        public void afterTextChanged(Editable s) {
        }
    };
}
