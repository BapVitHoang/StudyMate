package com.hcmute.studymate.controller;

import com.hcmute.studymate.model.Note;
import com.hcmute.studymate.service.SearchService;

import java.util.List;

public class SearchController {
    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    public List<Note> filterNotes(List<Note> notes, String keyword, String category) {
        return searchService.filterNotes(notes, keyword, category);
    }
}
