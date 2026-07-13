package com.hcmute.studymate.view;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.hcmute.studymate.R;
import com.hcmute.studymate.adapter.NoteAdapter;
import com.hcmute.studymate.controller.AuthController;
import com.hcmute.studymate.controller.CategoryController;
import com.hcmute.studymate.controller.NoteController;
import com.hcmute.studymate.controller.SearchController;
import com.hcmute.studymate.model.Category;
import com.hcmute.studymate.model.Note;
import com.hcmute.studymate.utils.AppContainer;
import com.hcmute.studymate.utils.Constants;
import com.hcmute.studymate.utils.DailyReviewScheduler;
import com.hcmute.studymate.utils.ListCallback;
import com.hcmute.studymate.utils.NoteTemplateUtils;
import com.hcmute.studymate.utils.OperationCallback;

import java.util.ArrayList;
import java.util.List;

public class NoteListActivity extends AppCompatActivity implements NoteAdapter.OnNoteClickListener {
    private AuthController authController;
    private NoteController noteController;
    private CategoryController categoryController;
    private SearchController searchController;
    private NoteAdapter noteAdapter;
    private TextInputEditText searchInput;
    private ChipGroup categoryChipGroup;
    private TextView emptyStateText;
    private ProgressBar notesLoadingProgress;

    private final List<Note> allNotes = new ArrayList<>();
    private final List<String> categoryNames = new ArrayList<>();
    private String selectedCategory = Constants.CATEGORY_ALL;
    private String userId;
    private boolean loadingNotes;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        authController = AppContainer.authController();
        if (!requireLoggedInUser()) {
            return;
        }
        setContentView(R.layout.activity_note_list);

        noteController = AppContainer.noteController();
        categoryController = AppContainer.categoryController();
        searchController = AppContainer.searchController();

        searchInput = findViewById(R.id.searchInput);
        categoryChipGroup = findViewById(R.id.categoryChipGroup);
        emptyStateText = findViewById(R.id.emptyStateText);
        notesLoadingProgress = findViewById(R.id.notesLoadingProgress);
        MaterialButton logoutButton = findViewById(R.id.logoutButton);
        MaterialButton statsButton = findViewById(R.id.statsButton);
        FloatingActionButton addNoteFab = findViewById(R.id.addNoteFab);
        RecyclerView notesRecyclerView = findViewById(R.id.notesRecyclerView);

        noteAdapter = new NoteAdapter(this);
        notesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        notesRecyclerView.setAdapter(noteAdapter);

        addNoteFab.setOnClickListener(view -> showNewNoteOptions());
        statsButton.setOnClickListener(view -> startActivity(new Intent(this, StudyStatsActivity.class)));
        logoutButton.setOnClickListener(view -> logout());
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                applyFilters();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
        categoryNames.add(Constants.CATEGORY_ALL);
        renderCategoryChips(false);
        DailyReviewScheduler.schedule(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!requireLoggedInUser()) {
            return;
        }
        loadCategories();
        loadNotes();
    }

    @Override
    public void onNoteClick(Note note) {
        startActivity(NoteDetailActivity.newIntent(this, note.getId()));
    }

    private void showNewNoteOptions() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_create_note, null);
        androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(this)
                .setView(dialogView)
                .create();
        dialogView.findViewById(R.id.blankNoteCard).setOnClickListener(view -> {
            dialog.dismiss();
            startActivity(NoteEditActivity.newIntent(this, null));
        });
        dialogView.findViewById(R.id.templateNoteCard).setOnClickListener(view -> {
            dialog.dismiss();
            showTemplateOptions();
        });
        dialog.show();
    }

    private void showTemplateOptions() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Choose template")
                .setItems(NoteTemplateUtils.TEMPLATE_NAMES, (dialog, which) -> {
                    String templateName = NoteTemplateUtils.TEMPLATE_NAMES[which];
                    startActivity(NoteEditActivity.newTemplateIntent(this, templateName));
                })
                .show();
    }

    private void loadCategories() {
        if (userId == null) {
            return;
        }
        renderCategoryChips(true);
        categoryController.loadCategories(userId, new ListCallback<Category>() {
            @Override
            public void onSuccess(List<Category> items) {
                categoryNames.clear();
                categoryNames.add(Constants.CATEGORY_ALL);
                for (Category category : items) {
                    if (category.getName() != null && !category.getName().trim().isEmpty()) {
                        categoryNames.add(category.getName());
                    }
                }
                if (!categoryNames.contains(selectedCategory)) {
                    selectedCategory = Constants.CATEGORY_ALL;
                }
                renderCategoryChips(false);
                applyFilters();
            }

            @Override
            public void onError(Exception exception) {
                renderCategoryChips(false);
                showError("Could not load categories", exception);
            }
        });
    }

    private void loadNotes() {
        if (userId == null) {
            return;
        }
        setNotesLoading(true);
        noteController.loadNotes(userId, new ListCallback<Note>() {
            @Override
            public void onSuccess(List<Note> items) {
                setNotesLoading(false);
                allNotes.clear();
                allNotes.addAll(items);
                applyFilters();
            }

            @Override
            public void onError(Exception exception) {
                setNotesLoading(false);
                showError("Could not load notes", exception);
            }
        });
    }

    private void applyFilters() {
        String keyword = searchInput.getText() == null ? "" : searchInput.getText().toString();
        List<Note> visibleNotes = searchController.filterNotes(allNotes, keyword, selectedCategory);
        noteAdapter.submitList(visibleNotes);
        if (loadingNotes) {
            emptyStateText.setVisibility(View.GONE);
            return;
        }

        emptyStateText.setText(buildEmptyStateText(keyword, visibleNotes));
        emptyStateText.setVisibility(visibleNotes.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void renderCategoryChips(boolean loading) {
        categoryChipGroup.removeAllViews();
        if (loading && categoryNames.size() <= 1) {
            Chip chip = buildCategoryChip("Loading", false);
            chip.setEnabled(false);
            categoryChipGroup.addView(chip);
            return;
        }

        for (String categoryName : categoryNames) {
            Chip chip = buildCategoryChip(categoryName, true);
            chip.setChecked(categoryName.equals(selectedCategory));
            chip.setOnClickListener(view -> {
                selectedCategory = categoryName;
                renderCategoryChips(false);
                applyFilters();
            });
            categoryChipGroup.addView(chip);
        }
    }

    private Chip buildCategoryChip(String label, boolean checkable) {
        Chip chip = new Chip(this);
        chip.setText(label);
        chip.setSingleLine(true);
        chip.setCheckable(checkable);
        chip.setClickable(checkable);
        chip.setChipCornerRadiusResource(R.dimen.chip_radius);
        chip.setChipStrokeWidthResource(R.dimen.chip_stroke_width);
        chip.setChipStrokeColorResource(R.color.studymate_outline);
        chip.setTextColor(getColor(R.color.studymate_chip_text));
        chip.setChipBackgroundColorResource(R.color.studymate_chip_background);
        return chip;
    }

    private boolean requireLoggedInUser() {
        userId = authController.getCurrentUserId();
        if (userId != null && !userId.trim().isEmpty()) {
            return true;
        }
        openLoginAndFinish();
        return false;
    }

    private void logout() {
        authController.signOut(new OperationCallback() {
            @Override
            public void onSuccess() {
                openLoginAndFinish();
            }

            @Override
            public void onError(Exception exception) {
                showError("Could not log out", exception);
            }
        });
    }

    private void openLoginAndFinish() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void setNotesLoading(boolean loading) {
        loadingNotes = loading;
        notesLoadingProgress.setVisibility(loading ? View.VISIBLE : View.GONE);
        if (loading) {
            emptyStateText.setVisibility(View.GONE);
        }
    }

    private String buildEmptyStateText(String keyword, List<Note> visibleNotes) {
        if (!visibleNotes.isEmpty()) {
            return "";
        }
        boolean hasKeyword = keyword != null && !keyword.trim().isEmpty();
        boolean filteredByCategory = !Constants.CATEGORY_ALL.equals(selectedCategory);
        if (allNotes.isEmpty()) {
            return getString(R.string.create_first_note);
        }
        if (hasKeyword || filteredByCategory) {
            return getString(R.string.no_matching_notes);
        }
        return getString(R.string.no_notes_found);
    }

    private void showError(String prefix, Exception exception) {
        String detail = exception == null || exception.getMessage() == null
                ? "Please check your connection and Firestore rules."
                : exception.getMessage();
        Toast.makeText(this, prefix + ". " + detail, Toast.LENGTH_LONG).show();
    }
}
