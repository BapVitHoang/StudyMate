package com.hcmute.studymate.controller;

import com.hcmute.studymate.model.Note;
import com.hcmute.studymate.service.NoteService;
import com.hcmute.studymate.utils.DataCallback;
import com.hcmute.studymate.utils.ListCallback;
import com.hcmute.studymate.utils.OperationCallback;

public class NoteController {
    private final NoteService noteService;

    public NoteController(NoteService noteService) {
        this.noteService = noteService;
    }

    public void loadNotes(String userId, ListCallback<Note> callback) {
        noteService.loadNotes(userId, callback);
    }

    public void loadNote(String userId, String noteId, DataCallback<Note> callback) {
        noteService.loadNote(userId, noteId, callback);
    }

    public void saveNote(String userId, Note note, OperationCallback callback) {
        noteService.saveNote(userId, note, callback);
    }

    public void deleteNote(String userId, String noteId, OperationCallback callback) {
        noteService.deleteNote(userId, noteId, callback);
    }
}
