package com.hcmute.studymate.database;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.hcmute.studymate.model.ChecklistItem;
import com.hcmute.studymate.model.Note;
import com.hcmute.studymate.model.VoiceRecording;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

@Entity(tableName = "notes")
public class NoteEntity {

    @PrimaryKey
    @NonNull
    private String id;
    private String userId;
    private String title;
    private String content;
    private String category;
    private String tagsJson;
    private String checklistJson;
    private long createdAt;
    private long updatedAt;
    private Long reminderAt;
    private String summary;
    private boolean pinned;
    private String status;
    private String recordingPath;
    private long recordingDurationMillis;
    private String recordingsJson;
    private String summaryBulletsJson;
    private String summaryKeyTermsJson;
    private Double summaryConfidence;
    private String summarySource;
    private Long summaryGeneratedAt;

    public NoteEntity() {
        this.id = "";
    }

    public static NoteEntity fromNote(Note note) {
        if (note == null) {
            return null;
        }
        NoteEntity entity = new NoteEntity();
        entity.id = note.getId() == null ? "" : note.getId();
        entity.userId = note.getUserId();
        entity.title = note.getTitle();
        entity.content = note.getContent();
        entity.category = note.getCategory();
        entity.tagsJson = encodeStringList(note.getTags());
        entity.checklistJson = encodeChecklist(note.getChecklist());
        entity.createdAt = note.getCreatedAt();
        entity.updatedAt = note.getUpdatedAt();
        entity.reminderAt = note.getReminderAt();
        entity.summary = note.getSummary();
        entity.pinned = note.isPinned();
        entity.status = note.getStatus();
        entity.recordingPath = note.getRecordingPath();
        entity.recordingDurationMillis = note.getRecordingDurationMillis();
        entity.recordingsJson = encodeRecordings(note.getRecordings());
        entity.summaryBulletsJson = encodeStringList(note.getSummaryBullets());
        entity.summaryKeyTermsJson = encodeStringList(note.getSummaryKeyTerms());
        entity.summaryConfidence = note.getSummaryConfidence();
        entity.summarySource = note.getSummarySource();
        entity.summaryGeneratedAt = note.getSummaryGeneratedAt();
        return entity;
    }

    public Note toNote() {
        Note note = new Note(id, userId, title, content, category);
        note.setCreatedAt(createdAt);
        note.setUpdatedAt(updatedAt);
        note.setReminderAt(reminderAt);
        note.setSummary(summary);
        note.setPinned(pinned);
        note.setStatus(status);
        note.setRecordingPath(recordingPath);
        note.setRecordingDurationMillis(recordingDurationMillis);
        note.setSummaryConfidence(summaryConfidence);
        note.setSummarySource(summarySource);
        note.setSummaryGeneratedAt(summaryGeneratedAt);

        note.setTags(decodeStringList(tagsJson));
        note.setChecklist(decodeChecklist(checklistJson));
        note.setRecordings(decodeRecordings(recordingsJson));
        note.setSummaryBullets(decodeStringList(summaryBulletsJson));
        note.setSummaryKeyTerms(decodeStringList(summaryKeyTermsJson));

        return note;
    }

    private static String encodeStringList(List<String> list) {
        if (list == null || list.isEmpty()) return "[]";
        JSONArray array = new JSONArray();
        for (String item : list) {
            if (item != null) array.put(item);
        }
        return array.toString();
    }

    private static List<String> decodeStringList(String json) {
        List<String> result = new ArrayList<>();
        if (json == null || json.trim().isEmpty()) return result;
        try {
            JSONArray array = new JSONArray(json);
            for (int i = 0; i < array.length(); i++) {
                result.add(array.getString(i));
            }
        } catch (Exception ignored) {
        }
        return result;
    }

    private static String encodeChecklist(List<ChecklistItem> list) {
        if (list == null || list.isEmpty()) return "[]";
        JSONArray array = new JSONArray();
        try {
            for (ChecklistItem item : list) {
                if (item != null) {
                    JSONObject obj = new JSONObject();
                    obj.put("text", item.getText());
                    obj.put("checked", item.isChecked());
                    array.put(obj);
                }
            }
        } catch (Exception ignored) {
        }
        return array.toString();
    }

    private static List<ChecklistItem> decodeChecklist(String json) {
        List<ChecklistItem> result = new ArrayList<>();
        if (json == null || json.trim().isEmpty()) return result;
        try {
            JSONArray array = new JSONArray(json);
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                result.add(new ChecklistItem(obj.optString("text"), obj.optBoolean("checked")));
            }
        } catch (Exception ignored) {
        }
        return result;
    }

    private static String encodeRecordings(List<VoiceRecording> list) {
        if (list == null || list.isEmpty()) return "[]";
        JSONArray array = new JSONArray();
        try {
            for (VoiceRecording rec : list) {
                if (rec != null) {
                    JSONObject obj = new JSONObject();
                    obj.put("path", rec.getPath());
                    obj.put("durationMillis", rec.getDurationMillis());
                    obj.put("createdAt", rec.getCreatedAt());
                    obj.put("label", rec.getLabel());
                    array.put(obj);
                }
            }
        } catch (Exception ignored) {
        }
        return array.toString();
    }

    private static List<VoiceRecording> decodeRecordings(String json) {
        List<VoiceRecording> result = new ArrayList<>();
        if (json == null || json.trim().isEmpty()) return result;
        try {
            JSONArray array = new JSONArray(json);
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                result.add(new VoiceRecording(
                        obj.optString("path"),
                        obj.optLong("durationMillis"),
                        obj.optLong("createdAt"),
                        obj.optString("label")
                ));
            }
        } catch (Exception ignored) {
        }
        return result;
    }

    @NonNull
    public String getId() { return id; }
    public void setId(@NonNull String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getTagsJson() { return tagsJson; }
    public void setTagsJson(String tagsJson) { this.tagsJson = tagsJson; }

    public String getChecklistJson() { return checklistJson; }
    public void setChecklistJson(String checklistJson) { this.checklistJson = checklistJson; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }

    public Long getReminderAt() { return reminderAt; }
    public void setReminderAt(Long reminderAt) { this.reminderAt = reminderAt; }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }

    public boolean isPinned() { return pinned; }
    public void setPinned(boolean pinned) { this.pinned = pinned; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getRecordingPath() { return recordingPath; }
    public void setRecordingPath(String recordingPath) { this.recordingPath = recordingPath; }

    public long getRecordingDurationMillis() { return recordingDurationMillis; }
    public void setRecordingDurationMillis(long recordingDurationMillis) { this.recordingDurationMillis = recordingDurationMillis; }

    public String getRecordingsJson() { return recordingsJson; }
    public void setRecordingsJson(String recordingsJson) { this.recordingsJson = recordingsJson; }

    public String getSummaryBulletsJson() { return summaryBulletsJson; }
    public void setSummaryBulletsJson(String summaryBulletsJson) { this.summaryBulletsJson = summaryBulletsJson; }

    public String getSummaryKeyTermsJson() { return summaryKeyTermsJson; }
    public void setSummaryKeyTermsJson(String summaryKeyTermsJson) { this.summaryKeyTermsJson = summaryKeyTermsJson; }

    public Double getSummaryConfidence() { return summaryConfidence; }
    public void setSummaryConfidence(Double summaryConfidence) { this.summaryConfidence = summaryConfidence; }

    public String getSummarySource() { return summarySource; }
    public void setSummarySource(String summarySource) { this.summarySource = summarySource; }

    public Long getSummaryGeneratedAt() { return summaryGeneratedAt; }
    public void setSummaryGeneratedAt(Long summaryGeneratedAt) { this.summaryGeneratedAt = summaryGeneratedAt; }
}
