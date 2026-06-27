package com.hcmute.studymate.repository;

import com.hcmute.studymate.model.Note;
import com.hcmute.studymate.utils.DataCallback;
import com.hcmute.studymate.utils.ListCallback;
import com.hcmute.studymate.utils.OperationCallback;

public interface NoteRepository {
    void getNotes(String userId, ListCallback<Note> callback);

    void getNoteById(String userId, String noteId, DataCallback<Note> callback);

    void saveNote(String userId, Note note, OperationCallback callback);

    void deleteNote(String userId, String noteId, OperationCallback callback);
}
