package com.hcmute.studymate;

import com.hcmute.studymate.ml.ContentHasher;
import com.hcmute.studymate.ml.NoteChunker;
import com.hcmute.studymate.ml.VectorMath;
import com.hcmute.studymate.model.Note;
import com.hcmute.studymate.model.NoteChunk;
import com.hcmute.studymate.model.QuizQuestion;
import com.hcmute.studymate.model.RetrievedChunk;
import com.hcmute.studymate.model.ReviewItem;
import com.hcmute.studymate.repository.LocalExtractiveRagRepository;
import com.hcmute.studymate.service.SrsService;
import com.hcmute.studymate.utils.Constants;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class RagBasicsTest {
    @Test
    public void contentHashIsStable() {
        assertEquals(ContentHasher.sha256("abc"), ContentHasher.sha256("abc"));
        assertFalse(ContentHasher.sha256("abc").equals(ContentHasher.sha256("abcd")));
    }

    @Test
    public void vectorNormalizeAndCosine() {
        float[] left = VectorMath.l2Normalize(new float[]{3f, 0f, 0f});
        float[] right = VectorMath.l2Normalize(new float[]{6f, 0f, 0f});
        assertEquals(1.0, VectorMath.cosineSimilarity(left, right), 1e-5);
    }

    @Test
    public void noteChunkerSplitsLongContent() {
        Note note = new Note();
        note.setTitle("Recursion");
        StringBuilder content = new StringBuilder();
        for (int i = 0; i < 30; i++) {
            content.append("Recursion solves problems by calling itself. Case ").append(i).append(". ");
        }
        note.setContent(content.toString());
        List<String> chunks = new NoteChunker().chunk(note);
        assertFalse(chunks.isEmpty());
        assertTrue(chunks.get(0).toLowerCase().contains("recursion"));
    }

    @Test
    public void extractiveQaUsesOverlappingSentences() {
        NoteChunk chunk = new NoteChunk();
        chunk.setNoteId("n1");
        chunk.setTitle("Binary Search");
        chunk.setText("Binary search finds an item in sorted arrays. It repeatedly halves the search space.");
        RetrievedChunk retrieved = new RetrievedChunk(chunk, 0.9, "test");
        List<RetrievedChunk> passages = new ArrayList<>();
        passages.add(retrieved);

        AtomicReference<com.hcmute.studymate.model.RagAnswer> answer = new AtomicReference<>();
        new LocalExtractiveRagRepository().answerFromNotes(
                "How does binary search find an item?",
                passages,
                "en",
                new com.hcmute.studymate.utils.DataCallback<com.hcmute.studymate.model.RagAnswer>() {
                    @Override
                    public void onSuccess(com.hcmute.studymate.model.RagAnswer data) {
                        answer.set(data);
                    }

                    @Override
                    public void onError(Exception exception) {
                        throw new AssertionError(exception);
                    }
                });

        assertTrue(answer.get() != null);
        assertTrue(answer.get().getAnswer().toLowerCase().contains("binary"));
        assertEquals(Constants.RAG_SOURCE_LOCAL, answer.get().getSource());
        assertTrue(answer.get().isUsedFallback());
    }

    @Test
    public void srsSchedulesGoodReview() {
        QuizQuestion question = new QuizQuestion();
        question.setId("q1");
        question.setStem("What is recursion?");
        question.setAnswer("A function calling itself");
        question.setType(QuizQuestion.TYPE_SHORT);

        SrsService srs = new SrsService();
        long now = 1_700_000_000_000L;
        ReviewItem item = srs.fromQuestion(question, now);
        srs.applyReview(item, SrsService.QUALITY_GOOD, now);
        assertEquals(1, item.getRepetitions());
        assertEquals(1, item.getIntervalDays());
        assertEquals(now + 24L * 60L * 60L * 1000L, item.getDueAt());
    }
}
