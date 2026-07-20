package com.hcmute.studymate.view;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
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
import com.hcmute.studymate.controller.RagController;
import com.hcmute.studymate.controller.SearchController;
import com.hcmute.studymate.ml.ModelManager;
import com.hcmute.studymate.model.Category;
import com.hcmute.studymate.model.Note;
import com.hcmute.studymate.model.RagAnswer;
import com.hcmute.studymate.model.RagCitation;
import com.hcmute.studymate.service.HybridSearchService;
import com.hcmute.studymate.utils.AppContainer;
import com.hcmute.studymate.utils.Constants;
import com.hcmute.studymate.utils.DailyReviewScheduler;
import com.hcmute.studymate.utils.DataCallback;
import com.hcmute.studymate.utils.ListCallback;
import com.hcmute.studymate.utils.NoteTemplateUtils;
import com.hcmute.studymate.utils.OperationCallback;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class NoteListActivity extends AppCompatActivity implements NoteAdapter.OnNoteClickListener {
    private AuthController authController;
    private NoteController noteController;
    private CategoryController categoryController;
    private SearchController searchController;
    private RagController ragController;
    private ModelManager modelManager;
    private NoteAdapter noteAdapter;
    private TextInputEditText searchInput;
    private ChipGroup categoryChipGroup;
    private TextView emptyStateText;
    private ProgressBar notesLoadingProgress;

    private final List<Note> allNotes = new ArrayList<>();
    private final List<String> categoryNames = new ArrayList<>();
    private final Handler searchHandler = new Handler(Looper.getMainLooper());
    private Runnable pendingSearch;
    private String selectedCategory = Constants.CATEGORY_ALL;
    private String userId;
    private boolean loadingNotes;
    private int searchGeneration;

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
        ragController = AppContainer.ragController();
        modelManager = AppContainer.modelManager();

        searchInput = findViewById(R.id.searchInput);
        categoryChipGroup = findViewById(R.id.categoryChipGroup);
        emptyStateText = findViewById(R.id.emptyStateText);
        notesLoadingProgress = findViewById(R.id.notesLoadingProgress);
        MaterialButton logoutButton = findViewById(R.id.logoutButton);
        MaterialButton statsButton = findViewById(R.id.statsButton);
        MaterialButton askNotesButton = findViewById(R.id.askNotesButton);
        FloatingActionButton addNoteFab = findViewById(R.id.addNoteFab);
        RecyclerView notesRecyclerView = findViewById(R.id.notesRecyclerView);

        noteAdapter = new NoteAdapter(this);
        notesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        notesRecyclerView.setAdapter(noteAdapter);

        addNoteFab.setOnClickListener(view -> showNewNoteOptions());
        statsButton.setOnClickListener(view -> startActivity(new Intent(this, StudyStatsActivity.class)));
        logoutButton.setOnClickListener(view -> logout());
        askNotesButton.setOnClickListener(view -> showAskDialog());
        askNotesButton.setOnLongClickListener(view -> {
            prepareModels();
            return true;
        });
        findViewById(R.id.openTutorButton).setOnClickListener(view ->
                startActivity(TutorActivity.newIntent(this)));
        findViewById(R.id.openExamPrepButton).setOnClickListener(view ->
                startActivity(ExamPrepActivity.newIntent(this)));
        findViewById(R.id.openQuizButton).setOnClickListener(view ->
                startActivity(QuizActivity.newIntent(this)));
        findViewById(R.id.openReviewButton).setOnClickListener(view ->
                startActivity(ReviewActivity.newIntent(this)));
        findViewById(R.id.openKnowledgeGraphButton).setOnClickListener(view ->
                startActivity(KnowledgeGraphActivity.newIntent(this)));
        findViewById(R.id.reindexButton).setOnClickListener(view -> reindexAllNotes());
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                scheduleHybridSearch();
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
    protected void onDestroy() {
        if (pendingSearch != null) {
            searchHandler.removeCallbacks(pendingSearch);
        }
        super.onDestroy();
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

    private void scheduleHybridSearch() {
        if (pendingSearch != null) {
            searchHandler.removeCallbacks(pendingSearch);
        }
        final int generation = ++searchGeneration;
        pendingSearch = () -> applyFilters(generation);
        searchHandler.postDelayed(pendingSearch, Constants.SEARCH_DEBOUNCE_MS);
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
                scheduleHybridSearch();
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
                scheduleHybridSearch();
            }

            @Override
            public void onError(Exception exception) {
                setNotesLoading(false);
                showError("Could not load notes", exception);
            }
        });
    }

    private void applyFilters(int generation) {
        String keyword = searchInput.getText() == null ? "" : searchInput.getText().toString();
        searchController.searchHybrid(userId, new ArrayList<>(allNotes), keyword, selectedCategory,
                new HybridSearchService.HybridCallback() {
                    @Override
                    public void onSuccess(List<Note> notes, boolean usedSemantic) {
                        if (generation != searchGeneration) {
                            return;
                        }
                        runOnUiThread(() -> {
                            noteAdapter.submitList(notes);
                            if (loadingNotes) {
                                emptyStateText.setVisibility(View.GONE);
                                return;
                            }
                            emptyStateText.setText(buildEmptyStateText(keyword, notes));
                            emptyStateText.setVisibility(notes.isEmpty() ? View.VISIBLE : View.GONE);
                        });
                    }

                    @Override
                    public void onError(Exception exception) {
                        if (generation != searchGeneration) {
                            return;
                        }
                        runOnUiThread(() -> {
                            List<Note> fallback = searchController.filterNotes(
                                    allNotes, keyword, selectedCategory);
                            noteAdapter.submitList(fallback);
                            emptyStateText.setText(buildEmptyStateText(keyword, fallback));
                            emptyStateText.setVisibility(fallback.isEmpty() ? View.VISIBLE : View.GONE);
                        });
                    }
                });
    }

    private void showAskDialog() {
        if (!modelManager.isReady()) {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.ask_notes_title)
                    .setMessage(R.string.rag_models_missing)
                    .setPositiveButton(R.string.download_rag_models, (dialog, which) -> prepareModels())
                    .setNegativeButton(R.string.cancel, null)
                    .show();
            return;
        }

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        input.setHint(R.string.ask_notes_hint);
        int padding = getResources().getDimensionPixelSize(R.dimen.space_lg);
        input.setPadding(padding, padding, padding, padding);

        new AlertDialog.Builder(this)
                .setTitle(R.string.ask_notes_title)
                .setView(input)
                .setPositiveButton(R.string.ask_notes_submit, (dialog, which) -> {
                    String question = input.getText() == null ? "" : input.getText().toString().trim();
                    if (question.isEmpty()) {
                        Toast.makeText(this, R.string.ask_notes_hint, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    askQuestion(question);
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void askQuestion(String question) {
        Toast.makeText(this, R.string.asking_notes, Toast.LENGTH_SHORT).show();
        ragController.ask(userId, question, Locale.getDefault().toLanguageTag(),
                new DataCallback<RagAnswer>() {
                    @Override
                    public void onSuccess(RagAnswer data) {
                        runOnUiThread(() -> showAskResult(data));
                    }

                    @Override
                    public void onError(Exception exception) {
                        runOnUiThread(() -> showError("Could not answer from notes", exception));
                    }
                });
    }

    private void showAskResult(RagAnswer answer) {
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        int padding = getResources().getDimensionPixelSize(R.dimen.space_lg);
        container.setPadding(padding, padding, padding, padding);

        TextView answerText = new TextView(this);
        answerText.setText(answer.getAnswer());
        answerText.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodyLarge);
        container.addView(answerText);

        if (answer.getCitations() != null && !answer.getCitations().isEmpty()) {
            TextView sourcesLabel = new TextView(this);
            sourcesLabel.setText(R.string.rag_citations);
            sourcesLabel.setPadding(0, padding, 0, padding / 2);
            sourcesLabel.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_TitleSmall);
            container.addView(sourcesLabel);

            for (RagCitation citation : answer.getCitations()) {
                MaterialButton button = new MaterialButton(this, null,
                        com.google.android.material.R.attr.materialButtonOutlinedStyle);
                String title = citation.getTitle() == null || citation.getTitle().isEmpty()
                        ? citation.getNoteId()
                        : citation.getTitle();
                String excerpt = citation.getExcerpt() == null ? "" : citation.getExcerpt();
                button.setText(title + (excerpt.isEmpty() ? "" : "\n" + excerpt));
                button.setOnClickListener(view -> {
                    if (citation.getNoteId() != null) {
                        startActivity(NoteDetailActivity.newIntent(this, citation.getNoteId()));
                    }
                });
                container.addView(button);
            }
        }

        new AlertDialog.Builder(this)
                .setTitle(R.string.ask_notes_title)
                .setView(container)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    private void prepareModels() {
        Toast.makeText(this, R.string.download_rag_models, Toast.LENGTH_SHORT).show();
        modelManager.ensureReadyAsync((ready, error) -> runOnUiThread(() -> {
            if (ready) {
                Toast.makeText(this, R.string.rag_models_ready, Toast.LENGTH_SHORT).show();
            } else {
                showError("Could not prepare RAG models", error);
            }
        }));
    }

    private void reindexAllNotes() {
        Toast.makeText(this, R.string.reindex_started, Toast.LENGTH_SHORT).show();
        AppContainer.indexingController().reindexAllAsync(userId, new ArrayList<>(allNotes),
                new OperationCallback() {
                    @Override
                    public void onSuccess() {
                        runOnUiThread(() ->
                                Toast.makeText(NoteListActivity.this, R.string.reindex_done,
                                        Toast.LENGTH_SHORT).show());
                    }

                    @Override
                    public void onError(Exception exception) {
                        runOnUiThread(() -> showError("Reindex failed", exception));
                    }
                });
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
                scheduleHybridSearch();
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
