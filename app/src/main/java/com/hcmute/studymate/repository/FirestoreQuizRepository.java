package com.hcmute.studymate.repository;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.hcmute.studymate.model.QuizQuestion;
import com.hcmute.studymate.model.ReviewItem;
import com.hcmute.studymate.utils.ListCallback;
import com.hcmute.studymate.utils.OperationCallback;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FirestoreQuizRepository {
    private final FirebaseFirestore firestore;

    public FirestoreQuizRepository() {
        this(FirebaseFirestore.getInstance());
    }

    FirestoreQuizRepository(FirebaseFirestore firestore) {
        this.firestore = firestore;
    }

    public void saveQuestions(String userId, List<QuizQuestion> questions, OperationCallback callback) {
        if (questions == null || questions.isEmpty()) {
            callback.onSuccess();
            return;
        }
        com.google.firebase.firestore.WriteBatch batch = firestore.batch();
        CollectionReference ref = cardsRef(userId);
        for (QuizQuestion question : questions) {
            String id = question.getId() == null || question.getId().isEmpty()
                    ? ref.document().getId() : question.getId();
            question.setId(id);
            batch.set(ref.document(id), questionToMap(question));
        }
        batch.commit()
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(callback::onError);
    }

    public void saveReviewItem(String userId, ReviewItem item, OperationCallback callback) {
        CollectionReference ref = reviewsRef(userId);
        String id = item.getId() == null || item.getId().isEmpty()
                ? ref.document().getId() : item.getId();
        item.setId(id);
        ref.document(id).set(reviewToMap(item))
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(callback::onError);
    }

    public void listDueReviews(String userId, long now, ListCallback<ReviewItem> callback) {
        reviewsRef(userId).whereLessThanOrEqualTo("dueAt", now)
                .orderBy("dueAt")
                .limit(50)
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<ReviewItem> items = new ArrayList<>();
                    for (DocumentSnapshot document : snapshot.getDocuments()) {
                        ReviewItem item = reviewFromDocument(document);
                        if (item != null) {
                            items.add(item);
                        }
                    }
                    callback.onSuccess(items);
                })
                .addOnFailureListener(callback::onError);
    }

    public void listAllReviews(String userId, ListCallback<ReviewItem> callback) {
        reviewsRef(userId).orderBy("dueAt").limit(100).get()
                .addOnSuccessListener(snapshot -> {
                    List<ReviewItem> items = new ArrayList<>();
                    for (DocumentSnapshot document : snapshot.getDocuments()) {
                        ReviewItem item = reviewFromDocument(document);
                        if (item != null) {
                            items.add(item);
                        }
                    }
                    callback.onSuccess(items);
                })
                .addOnFailureListener(callback::onError);
    }

    private Map<String, Object> questionToMap(QuizQuestion question) {
        Map<String, Object> data = new HashMap<>();
        data.put("type", question.getType());
        data.put("stem", question.getStem());
        data.put("choices", question.getChoices());
        data.put("answer", question.getAnswer());
        data.put("explanation", question.getExplanation());
        data.put("sourceNoteId", question.getSourceNoteId());
        data.put("sourceChunkId", question.getSourceChunkId());
        return data;
    }

    private Map<String, Object> reviewToMap(ReviewItem item) {
        Map<String, Object> data = new HashMap<>();
        data.put("questionId", item.getQuestionId());
        data.put("stem", item.getStem());
        data.put("answer", item.getAnswer());
        data.put("explanation", item.getExplanation());
        data.put("sourceNoteId", item.getSourceNoteId());
        data.put("type", item.getType());
        data.put("easeFactor", item.getEaseFactor());
        data.put("intervalDays", item.getIntervalDays());
        data.put("repetitions", item.getRepetitions());
        data.put("dueAt", item.getDueAt());
        data.put("updatedAt", item.getUpdatedAt());
        return data;
    }

    private ReviewItem reviewFromDocument(DocumentSnapshot document) {
        if (document == null || !document.exists()) {
            return null;
        }
        ReviewItem item = new ReviewItem();
        item.setId(document.getId());
        item.setQuestionId(document.getString("questionId"));
        item.setStem(document.getString("stem"));
        item.setAnswer(document.getString("answer"));
        item.setExplanation(document.getString("explanation"));
        item.setSourceNoteId(document.getString("sourceNoteId"));
        item.setType(document.getString("type"));
        Double ease = document.getDouble("easeFactor");
        item.setEaseFactor(ease == null ? 2.5 : ease);
        Long interval = document.getLong("intervalDays");
        item.setIntervalDays(interval == null ? 0 : interval.intValue());
        Long reps = document.getLong("repetitions");
        item.setRepetitions(reps == null ? 0 : reps.intValue());
        Long dueAt = document.getLong("dueAt");
        item.setDueAt(dueAt == null ? 0L : dueAt);
        Long updatedAt = document.getLong("updatedAt");
        item.setUpdatedAt(updatedAt == null ? 0L : updatedAt);
        return item;
    }

    private CollectionReference cardsRef(String userId) {
        return firestore.collection("users").document(userId).collection("quizCards");
    }

    private CollectionReference reviewsRef(String userId) {
        return firestore.collection("users").document(userId).collection("reviewItems");
    }
}
