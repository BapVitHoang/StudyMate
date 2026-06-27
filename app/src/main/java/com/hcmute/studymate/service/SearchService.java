package com.hcmute.studymate.service;

import com.hcmute.studymate.model.Note;
import com.hcmute.studymate.utils.Constants;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SearchService {
    public List<Note> filterNotes(List<Note> notes, String keyword, String category) {
        List<Note> result = new ArrayList<>();
        String normalizedKeyword = keyword == null ? "" : keyword.trim().toLowerCase(Locale.US);
        String normalizedCategory = category == null ? Constants.CATEGORY_ALL : category;

        for (Note note : notes) {
            boolean matchesCategory = Constants.CATEGORY_ALL.equals(normalizedCategory)
                    || normalizedCategory.equals(note.getCategory());
            boolean matchesKeyword = normalizedKeyword.isEmpty() || containsKeyword(note, normalizedKeyword);
            if (matchesCategory && matchesKeyword) {
                result.add(note);
            }
        }
        return result;
    }

    private boolean containsKeyword(Note note, String keyword) {
        if (contains(note.getTitle(), keyword) || contains(note.getContent(), keyword)
                || contains(note.getCategory(), keyword)) {
            return true;
        }

        if (note.getTags() == null) {
            return false;
        }
        for (String tag : note.getTags()) {
            if (contains(tag, keyword)) {
                return true;
            }
        }
        return false;
    }

    private boolean contains(String value, String keyword) {
        return value != null && value.toLowerCase(Locale.US).contains(keyword);
    }
}
