package com.hcmute.studymate.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface NoteDao {

    @Query("SELECT * FROM notes WHERE userId = :userId ORDER BY pinned DESC, updatedAt DESC")
    List<NoteEntity> getNotesByUserId(String userId);

    @Query("SELECT * FROM notes WHERE id = :noteId LIMIT 1")
    NoteEntity getNoteById(String noteId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertOrUpdate(NoteEntity note);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertOrUpdateAll(List<NoteEntity> notes);

    @Query("DELETE FROM notes WHERE id = :noteId")
    void deleteById(String noteId);

    @Query("DELETE FROM notes WHERE userId = :userId")
    void deleteByUserId(String userId);
}
