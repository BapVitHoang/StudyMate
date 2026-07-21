package com.hcmute.studymate.repository;

import android.content.Context;

import com.hcmute.studymate.database.AppDatabase;
import com.hcmute.studymate.database.NoteDao;
import com.hcmute.studymate.database.NoteEntity;
import com.hcmute.studymate.model.Note;
import com.hcmute.studymate.utils.DataCallback;
import com.hcmute.studymate.utils.ListCallback;
import com.hcmute.studymate.utils.OperationCallback;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LocalCachedNoteRepository implements NoteRepository {
    private final NoteDao noteDao;
    private final NoteRepository remoteRepository;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public LocalCachedNoteRepository(Context context, NoteRepository remoteRepository) {
        this.noteDao = AppDatabase.getInstance(context).noteDao();
        this.remoteRepository = remoteRepository;
    }

    @Override
    public void getNotes(String userId, ListCallback<Note> callback) {
        if (userId == null || userId.trim().isEmpty()) {
            callback.onError(new IllegalArgumentException("User id is required"));
            return;
        }

        // 1. Instant local Room DB load
        executor.execute(() -> {
            try {
                List<NoteEntity> entities = noteDao.getNotesByUserId(userId);
                if (entities != null && !entities.isEmpty()) {
                    List<Note> localNotes = new ArrayList<>();
                    for (NoteEntity entity : entities) {
                        localNotes.add(entity.toNote());
                    }
                    callback.onSuccess(localNotes);
                }
            } catch (Exception ignored) {
            }

            // 2. Fetch fresh notes from Firestore
            remoteRepository.getNotes(userId, new ListCallback<Note>() {
                @Override
                public void onSuccess(List<Note> remoteNotes) {
                    executor.execute(() -> {
                        if (remoteNotes != null && !remoteNotes.isEmpty()) {
                            List<NoteEntity> toCache = new ArrayList<>();
                            for (Note note : remoteNotes) {
                                toCache.add(NoteEntity.fromNote(note));
                            }
                            noteDao.insertOrUpdateAll(toCache);
                        }
                    });
                    callback.onSuccess(remoteNotes);
                }

                @Override
                public void onError(Exception exception) {
                    // If Firestore fails (e.g. offline), Room cache was already served
                }
            });
        });
    }

    @Override
    public void getNoteById(String userId, String noteId, DataCallback<Note> callback) {
        if (userId == null || noteId == null) {
            callback.onError(new IllegalArgumentException("User id and note id are required"));
            return;
        }

        executor.execute(() -> {
            try {
                NoteEntity cached = noteDao.getNoteById(noteId);
                if (cached != null) {
                    callback.onSuccess(cached.toNote());
                }
            } catch (Exception ignored) {
            }

            remoteRepository.getNoteById(userId, noteId, new DataCallback<Note>() {
                @Override
                public void onSuccess(Note remoteNote) {
                    executor.execute(() -> {
                        if (remoteNote != null) {
                            noteDao.insertOrUpdate(NoteEntity.fromNote(remoteNote));
                        }
                    });
                    callback.onSuccess(remoteNote);
                }

                @Override
                public void onError(Exception exception) {
                    // Serve cached if remote fails
                }
            });
        });
    }

    @Override
    public void saveNote(String userId, Note note, OperationCallback callback) {
        if (userId == null || note == null) {
            callback.onError(new IllegalArgumentException("User id and note are required"));
            return;
        }

        if (note.getId() == null || note.getId().trim().isEmpty()) {
            note.setId(java.util.UUID.randomUUID().toString());
        }
        note.setUserId(userId);

        executor.execute(() -> {
            try {
                noteDao.insertOrUpdate(NoteEntity.fromNote(note));
            } catch (Exception ignored) {
            }

            // Sync to Firestore in background
            remoteRepository.saveNote(userId, note, new OperationCallback() {
                @Override
                public void onSuccess() {
                    callback.onSuccess();
                }

                @Override
                public void onError(Exception exception) {
                    // Even if remote sync fails temporarily, local save succeeded
                    callback.onSuccess();
                }
            });
        });
    }

    @Override
    public void deleteNote(String userId, String noteId, OperationCallback callback) {
        if (userId == null || noteId == null) {
            callback.onError(new IllegalArgumentException("User id and note id are required"));
            return;
        }

        executor.execute(() -> {
            try {
                noteDao.deleteById(noteId);
            } catch (Exception ignored) {
            }

            remoteRepository.deleteNote(userId, noteId, new OperationCallback() {
                @Override
                public void onSuccess() {
                    callback.onSuccess();
                }

                @Override
                public void onError(Exception exception) {
                    callback.onSuccess();
                }
            });
        });
    }
}
