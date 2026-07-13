package com.hcmute.studymate.service;

import com.hcmute.studymate.model.Note;
import com.hcmute.studymate.repository.NoteRepository;
import com.hcmute.studymate.utils.DataCallback;
import com.hcmute.studymate.utils.ListCallback;
import com.hcmute.studymate.utils.OperationCallback;

public class NoteService {
    private final NoteRepository noteRepository;

    public NoteService(NoteRepository noteRepository) {
        this.noteRepository = noteRepository;
    }

    public void loadNotes(String userId, ListCallback<Note> callback) {
        noteRepository.getNotes(userId, callback);
    }

    public void loadNote(String userId, String noteId, DataCallback<Note> callback) {
        if (isBlank(noteId)) {
            callback.onError(new IllegalArgumentException("Note id is required"));
            return;
        }
        noteRepository.getNoteById(userId, noteId, callback);
    }

    public void saveNote(String userId, Note note, OperationCallback callback) {
        if (note == null) {
            callback.onError(new IllegalArgumentException("Note is required"));
            return;
        }
        if (isBlank(note.getTitle())) {
            callback.onError(new IllegalArgumentException("Title is required"));
            return;
        }
        if (isBlank(note.getContent())) {
            callback.onError(new IllegalArgumentException("Content is required"));
            return;
        }

        long now = System.currentTimeMillis();
        if (note.getCreatedAt() == 0L) {
            note.setCreatedAt(now);
        }
        note.setUpdatedAt(now);
        noteRepository.saveNote(userId, note, callback);
    }

    public void deleteNote(String userId, String noteId, OperationCallback callback) {
        if (isBlank(noteId)) {
            callback.onError(new IllegalArgumentException("Note id is required"));
            return;
        }
        noteRepository.deleteNote(userId, noteId, callback);
    }

    public void setPinned(String userId, Note note, boolean pinned, OperationCallback callback) {
        if (note == null) {
            callback.onError(new IllegalArgumentException("Note is required"));
            return;
        }
        note.setPinned(pinned);
        note.setUpdatedAt(System.currentTimeMillis());
        noteRepository.saveNote(userId, note, callback);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
