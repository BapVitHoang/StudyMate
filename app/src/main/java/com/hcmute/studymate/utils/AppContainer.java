package com.hcmute.studymate.utils;

import android.content.Context;

import com.hcmute.studymate.controller.AuthController;
import com.hcmute.studymate.controller.CategoryController;
import com.hcmute.studymate.controller.ExamPrepController;
import com.hcmute.studymate.controller.IndexingController;
import com.hcmute.studymate.controller.KnowledgeGraphController;
import com.hcmute.studymate.controller.NoteController;
import com.hcmute.studymate.controller.QuizController;
import com.hcmute.studymate.controller.RagController;
import com.hcmute.studymate.controller.ReminderController;
import com.hcmute.studymate.controller.SearchController;
import com.hcmute.studymate.controller.SummaryController;
import com.hcmute.studymate.controller.StudySessionController;
import com.hcmute.studymate.controller.TutorController;
import com.hcmute.studymate.ml.LocalEmbeddingEngine;
import com.hcmute.studymate.ml.LocalReranker;
import com.hcmute.studymate.ml.ModelManager;
import com.hcmute.studymate.ml.NoteChunker;
import com.hcmute.studymate.repository.AiSummaryRepository;
import com.hcmute.studymate.repository.AuthRepository;
import com.hcmute.studymate.repository.CategoryRepository;
import com.hcmute.studymate.repository.ChunkRepository;
import com.hcmute.studymate.repository.CloudAiSummaryRepository;
import com.hcmute.studymate.repository.CloudConceptRepository;
import com.hcmute.studymate.repository.CloudExamPrepRepository;
import com.hcmute.studymate.repository.CloudQuizRepository;
import com.hcmute.studymate.repository.CloudRagRepository;
import com.hcmute.studymate.repository.CloudTutorRepository;
import com.hcmute.studymate.repository.FirebaseAuthRepository;
import com.hcmute.studymate.repository.FirestoreCategoryRepository;
import com.hcmute.studymate.repository.FirestoreChunkRepository;
import com.hcmute.studymate.repository.FirestoreConceptRepository;
import com.hcmute.studymate.repository.FirestoreExamPrepRepository;
import com.hcmute.studymate.repository.FirestoreNoteRepository;
import com.hcmute.studymate.repository.FirestoreQuizRepository;
import com.hcmute.studymate.repository.FirestoreReminderRepository;
import com.hcmute.studymate.repository.FirestoreStudySessionRepository;
import com.hcmute.studymate.repository.FirestoreTutorSessionRepository;
import com.hcmute.studymate.repository.HybridAiSummaryRepository;
import com.hcmute.studymate.repository.HybridRagRepository;
import com.hcmute.studymate.repository.LocalAiSummaryRepository;
import com.hcmute.studymate.repository.LocalExtractiveRagRepository;
import com.hcmute.studymate.repository.NoteRepository;
import com.hcmute.studymate.repository.ReminderRepository;
import com.hcmute.studymate.repository.StudySessionRepository;
import com.hcmute.studymate.service.AuthService;
import com.hcmute.studymate.service.CategoryService;
import com.hcmute.studymate.service.ExamPrepService;
import com.hcmute.studymate.service.HybridSearchService;
import com.hcmute.studymate.service.IndexingService;
import com.hcmute.studymate.service.KnowledgeGraphService;
import com.hcmute.studymate.service.NoteService;
import com.hcmute.studymate.service.QuizService;
import com.hcmute.studymate.service.RagService;
import com.hcmute.studymate.service.ReminderService;
import com.hcmute.studymate.service.SearchService;
import com.hcmute.studymate.service.SrsService;
import com.hcmute.studymate.service.SummaryService;
import com.hcmute.studymate.service.StudySessionService;
import com.hcmute.studymate.service.TutorService;

public final class AppContainer {
    private static final AuthRepository AUTH_REPOSITORY = new FirebaseAuthRepository();
    private static final NoteRepository NOTE_REPOSITORY = new FirestoreNoteRepository();
    private static final CategoryRepository CATEGORY_REPOSITORY = new FirestoreCategoryRepository();
    private static final ReminderRepository REMINDER_REPOSITORY = new FirestoreReminderRepository();
    private static final StudySessionRepository STUDY_SESSION_REPOSITORY = new FirestoreStudySessionRepository();
    private static final ChunkRepository CHUNK_REPOSITORY = new FirestoreChunkRepository();
    private static final AiSummaryRepository AI_SUMMARY_REPOSITORY = new HybridAiSummaryRepository(
            new CloudAiSummaryRepository(),
            new LocalAiSummaryRepository()
    );
    private static final LocalExtractiveRagRepository LOCAL_EXTRACTIVE_RAG_REPOSITORY =
            new LocalExtractiveRagRepository();
    private static final HybridRagRepository HYBRID_RAG_REPOSITORY = new HybridRagRepository(
            new CloudRagRepository(),
            LOCAL_EXTRACTIVE_RAG_REPOSITORY
    );
    private static final CloudTutorRepository CLOUD_TUTOR_REPOSITORY = new CloudTutorRepository();
    private static final FirestoreTutorSessionRepository TUTOR_SESSION_REPOSITORY =
            new FirestoreTutorSessionRepository();
    private static final CloudExamPrepRepository CLOUD_EXAM_PREP_REPOSITORY = new CloudExamPrepRepository();
    private static final FirestoreExamPrepRepository EXAM_PREP_REPOSITORY = new FirestoreExamPrepRepository();
    private static final CloudQuizRepository CLOUD_QUIZ_REPOSITORY = new CloudQuizRepository();
    private static final FirestoreQuizRepository QUIZ_REPOSITORY = new FirestoreQuizRepository();
    private static final CloudConceptRepository CLOUD_CONCEPT_REPOSITORY = new CloudConceptRepository();
    private static final FirestoreConceptRepository CONCEPT_REPOSITORY = new FirestoreConceptRepository();

    private static ModelManager modelManager;
    private static LocalEmbeddingEngine embeddingEngine;
    private static LocalReranker reranker;
    private static IndexingService indexingService;
    private static HybridSearchService hybridSearchService;
    private static RagService ragService;
    private static TutorService tutorService;
    private static ExamPrepService examPrepService;
    private static QuizService quizService;
    private static KnowledgeGraphService knowledgeGraphService;
    private static volatile boolean initialized;

    private AppContainer() {
    }

    public static synchronized void init(Context context) {
        if (initialized) {
            return;
        }
        modelManager = new ModelManager(context);
        embeddingEngine = new LocalEmbeddingEngine(modelManager);
        reranker = new LocalReranker(modelManager);
        indexingService = new IndexingService(
                CHUNK_REPOSITORY, modelManager, embeddingEngine, new NoteChunker());
        hybridSearchService = new HybridSearchService(
                new SearchService(), CHUNK_REPOSITORY, modelManager, embeddingEngine, reranker);
        ragService = new RagService(hybridSearchService, HYBRID_RAG_REPOSITORY);
        tutorService = new TutorService(
                hybridSearchService, CLOUD_TUTOR_REPOSITORY,
                LOCAL_EXTRACTIVE_RAG_REPOSITORY, TUTOR_SESSION_REPOSITORY);
        examPrepService = new ExamPrepService(
                hybridSearchService, CLOUD_EXAM_PREP_REPOSITORY, EXAM_PREP_REPOSITORY);
        quizService = new QuizService(
                hybridSearchService, CLOUD_QUIZ_REPOSITORY, QUIZ_REPOSITORY, new SrsService());
        knowledgeGraphService = new KnowledgeGraphService(
                CLOUD_CONCEPT_REPOSITORY, CONCEPT_REPOSITORY, modelManager, embeddingEngine);
        initialized = true;
        modelManager.ensureReadyAsync((ready, error) -> {
            // Best-effort warm-up; keyword search remains available if models are missing.
        });
    }

    private static void requireInit() {
        if (!initialized) {
            throw new IllegalStateException("AppContainer.init(context) must be called first");
        }
    }

    public static AuthController authController() {
        return new AuthController(new AuthService(AUTH_REPOSITORY));
    }

    public static NoteController noteController() {
        requireInit();
        return new NoteController(new NoteService(NOTE_REPOSITORY, indexingService));
    }

    public static CategoryController categoryController() {
        return new CategoryController(new CategoryService(CATEGORY_REPOSITORY));
    }

    public static SearchController searchController() {
        requireInit();
        return new SearchController(new SearchService(), hybridSearchService);
    }

    public static ReminderController reminderController() {
        return new ReminderController(new ReminderService(REMINDER_REPOSITORY, NOTE_REPOSITORY));
    }

    public static SummaryController summaryController() {
        return new SummaryController(new SummaryService(AI_SUMMARY_REPOSITORY));
    }

    public static StudySessionController studySessionController() {
        return new StudySessionController(new StudySessionService(STUDY_SESSION_REPOSITORY));
    }

    public static IndexingController indexingController() {
        requireInit();
        return new IndexingController(indexingService);
    }

    public static RagController ragController() {
        requireInit();
        return new RagController(ragService);
    }

    public static TutorController tutorController() {
        requireInit();
        return new TutorController(tutorService);
    }

    public static ExamPrepController examPrepController() {
        requireInit();
        return new ExamPrepController(examPrepService);
    }

    public static QuizController quizController() {
        requireInit();
        return new QuizController(quizService);
    }

    public static KnowledgeGraphController knowledgeGraphController() {
        requireInit();
        return new KnowledgeGraphController(knowledgeGraphService);
    }

    public static ModelManager modelManager() {
        requireInit();
        return modelManager;
    }
}
