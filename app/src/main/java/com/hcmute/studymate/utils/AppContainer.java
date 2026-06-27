package com.hcmute.studymate.utils;

import com.hcmute.studymate.controller.AuthController;
import com.hcmute.studymate.controller.CategoryController;
import com.hcmute.studymate.controller.NoteController;
import com.hcmute.studymate.controller.ReminderController;
import com.hcmute.studymate.controller.SearchController;
import com.hcmute.studymate.controller.SummaryController;
import com.hcmute.studymate.repository.AiSummaryRepository;
import com.hcmute.studymate.repository.AuthRepository;
import com.hcmute.studymate.repository.CategoryRepository;
import com.hcmute.studymate.repository.FirebaseAuthRepository;
import com.hcmute.studymate.repository.FirestoreCategoryRepository;
import com.hcmute.studymate.repository.FirestoreNoteRepository;
import com.hcmute.studymate.repository.FirestoreReminderRepository;
import com.hcmute.studymate.repository.LocalAiSummaryRepository;
import com.hcmute.studymate.repository.NoteRepository;
import com.hcmute.studymate.repository.ReminderRepository;
import com.hcmute.studymate.service.AuthService;
import com.hcmute.studymate.service.CategoryService;
import com.hcmute.studymate.service.NoteService;
import com.hcmute.studymate.service.ReminderService;
import com.hcmute.studymate.service.SearchService;
import com.hcmute.studymate.service.SummaryService;

public final class AppContainer {
    private static final AuthRepository AUTH_REPOSITORY = new FirebaseAuthRepository();
    private static final NoteRepository NOTE_REPOSITORY = new FirestoreNoteRepository();
    private static final CategoryRepository CATEGORY_REPOSITORY = new FirestoreCategoryRepository();
    private static final ReminderRepository REMINDER_REPOSITORY = new FirestoreReminderRepository();
    private static final AiSummaryRepository AI_SUMMARY_REPOSITORY = new LocalAiSummaryRepository();

    private AppContainer() {
    }

    public static AuthController authController() {
        return new AuthController(new AuthService(AUTH_REPOSITORY));
    }

    public static NoteController noteController() {
        return new NoteController(new NoteService(NOTE_REPOSITORY));
    }

    public static CategoryController categoryController() {
        return new CategoryController(new CategoryService(CATEGORY_REPOSITORY));
    }

    public static SearchController searchController() {
        return new SearchController(new SearchService());
    }

    public static ReminderController reminderController() {
        return new ReminderController(new ReminderService(REMINDER_REPOSITORY, NOTE_REPOSITORY));
    }

    public static SummaryController summaryController() {
        return new SummaryController(new SummaryService(AI_SUMMARY_REPOSITORY));
    }
}
