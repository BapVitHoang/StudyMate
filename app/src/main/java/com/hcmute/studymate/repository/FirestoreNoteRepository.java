package com.hcmute.studymate.repository;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.SetOptions;
import com.hcmute.studymate.model.Note;
import com.hcmute.studymate.utils.DataCallback;
import com.hcmute.studymate.utils.ListCallback;
import com.hcmute.studymate.utils.OperationCallback;

import java.util.ArrayList;
import java.util.List;

public class FirestoreNoteRepository implements NoteRepository {
    private static final String USERS_COLLECTION = "users";
    private static final String NOTES_COLLECTION = "notes";

    private final FirebaseFirestore firestore;

    public FirestoreNoteRepository() {
        this(FirebaseFirestore.getInstance());
    }

    FirestoreNoteRepository(FirebaseFirestore firestore) {
        this.firestore = firestore;
    }

    @Override
    public void getNotes(String userId, ListCallback<Note> callback) {
        if (isBlank(userId)) {
            callback.onError(new IllegalArgumentException("User id is required"));
            return;
        }

        notesRef(userId)
                .orderBy("updatedAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Note> notes = new ArrayList<>();
                    for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                        Note note = document.toObject(Note.class);
                        if (note != null) {
                            hydrateDocumentFields(userId, document, note);
                            notes.add(note);
                        }
                    }
                    notes.sort((first, second) -> {
                        if (first.isPinned() != second.isPinned()) {
                            return first.isPinned() ? -1 : 1;
                        }
                        return Long.compare(second.getUpdatedAt(), first.getUpdatedAt());
                    });
                    callback.onSuccess(notes);
                })
                .addOnFailureListener(callback::onError);
    }

    @Override
    public void getNoteById(String userId, String noteId, DataCallback<Note> callback) {
        if (isBlank(userId) || isBlank(noteId)) {
            callback.onError(new IllegalArgumentException("User id and note id are required"));
            return;
        }

        notesRef(userId)
                .document(noteId)
                .get()
                .addOnSuccessListener(document -> {
                    if (!document.exists()) {
                        callback.onError(new IllegalArgumentException("Note not found"));
                        return;
                    }

                    Note note = document.toObject(Note.class);
                    if (note == null) {
                        callback.onError(new IllegalStateException("Unable to read note data"));
                        return;
                    }

                    hydrateDocumentFields(userId, document, note);
                    callback.onSuccess(note);
                })
                .addOnFailureListener(callback::onError);
    }

    @Override
    public void saveNote(String userId, Note note, OperationCallback callback) {
        if (isBlank(userId) || note == null) {
            callback.onError(new IllegalArgumentException("User id and note are required"));
            return;
        }

        CollectionReference notesRef = notesRef(userId);
        if (isBlank(note.getId())) {
            note.setId(notesRef.document().getId());
        }
        note.setUserId(userId);

        notesRef.document(note.getId())
                .set(note, SetOptions.merge())
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(callback::onError);
    }

    @Override
    public void deleteNote(String userId, String noteId, OperationCallback callback) {
        if (isBlank(userId) || isBlank(noteId)) {
            callback.onError(new IllegalArgumentException("User id and note id are required"));
            return;
        }

        notesRef(userId)
                .document(noteId)
                .delete()
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(callback::onError);
    }

    private CollectionReference notesRef(String userId) {
        return firestore.collection(USERS_COLLECTION)
                .document(userId)
                .collection(NOTES_COLLECTION);
    }

    private void hydrateDocumentFields(String userId, DocumentSnapshot document, Note note) {
        if (isBlank(note.getId())) {
            note.setId(document.getId());
        }
        if (isBlank(note.getUserId())) {
            note.setUserId(userId);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
