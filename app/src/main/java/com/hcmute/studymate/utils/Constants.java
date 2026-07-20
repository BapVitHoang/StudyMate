package com.hcmute.studymate.utils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class Constants {
    public static final String CATEGORY_ALL = "All";
    public static final String CATEGORY_GENERAL = "General";
    public static final String SUMMARY_SOURCE_LOCAL = "local-rule-based";
    public static final String STATUS_NEW = "New";
    public static final String STATUS_LEARNING = "Learning";
    public static final String STATUS_REVIEWED = "Reviewed";
    public static final String STATUS_MASTERED = "Mastered";
    public static final String SUMMARY_SOURCE_CLOUD = "gemini-cloud";
    public static final String RAG_SOURCE_LOCAL = "local-extractive";
    public static final String RAG_SOURCE_CLOUD = "gemini-cloud";
    public static final String CALLABLE_SUMMARIZE_NOTE = "summarizeNote";
    public static final String CALLABLE_ANSWER_FROM_NOTES = "answerFromNotes";
    public static final String CALLABLE_TUTOR_CHAT = "tutorChat";
    public static final String CALLABLE_SYNTHESIZE_EXAM_PREP = "synthesizeExamPrep";
    public static final String CALLABLE_GENERATE_QUIZ = "generateQuiz";
    public static final String CALLABLE_EXTRACT_CONCEPTS = "extractConcepts";

    public static final String EMBED_MODEL_ID = "all-minilm-l6-v2-onnx";
    public static final String RERANK_MODEL_ID = "ms-marco-minilm-l6-v2-onnx";
    public static final int EMBEDDING_DIMENSIONS = 384;
    public static final int CHUNK_TARGET_CHARS = 450;
    public static final int CHUNK_OVERLAP_CHARS = 80;
    public static final int MAX_CHUNKS_PER_NOTE = 40;
    public static final int VECTOR_TOP_K = 20;
    public static final int EXAM_PREP_VECTOR_TOP_K = 40;
    public static final int RERANK_TOP_N = 8;
    public static final int RAG_PASSAGE_COUNT = 5;
    public static final int TUTOR_PASSAGE_COUNT = 6;
    public static final int EXAM_PREP_PASSAGE_COUNT = 10;
    public static final int QUIZ_PASSAGE_COUNT = 8;
    public static final int RRF_K = 60;
    public static final int SEARCH_DEBOUNCE_MS = 300;
    public static final int EMBED_MAX_SEQ_LEN = 128;
    public static final int RERANK_MAX_SEQ_LEN = 256;
    public static final int TUTOR_MAX_HISTORY = 12;
    public static final int DEFAULT_QUIZ_COUNT = 5;

    /** Override these after uploading exported ONNX files to Firebase Storage / CDN. */
    public static final String DEFAULT_EMBED_MODEL_URL =
            "https://huggingface.co/sentence-transformers/all-MiniLM-L6-v2/resolve/main/onnx/model_qint8_avx512_vnni.onnx";
    public static final String DEFAULT_RERANK_MODEL_URL =
            "https://huggingface.co/cross-encoder/ms-marco-MiniLM-L-6-v2/resolve/main/onnx/model_qint8_avx512_vnni.onnx";
    public static final String DEFAULT_VOCAB_URL =
            "https://huggingface.co/sentence-transformers/all-MiniLM-L6-v2/resolve/main/vocab.txt";

    public static final List<String> DEFAULT_CATEGORY_NAMES = Collections.unmodifiableList(
            Arrays.asList(CATEGORY_GENERAL, "Math", "English", "Programming")
    );

    public static final List<String> NOTE_STATUSES = Collections.unmodifiableList(
            Arrays.asList(STATUS_NEW, STATUS_LEARNING, STATUS_REVIEWED, STATUS_MASTERED)
    );

    private Constants() {
    }
}
