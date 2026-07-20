package com.hcmute.studymate.controller;

import com.hcmute.studymate.model.Note;
import com.hcmute.studymate.service.HybridSearchService;
import com.hcmute.studymate.service.SearchService;

import java.util.List;

public class SearchController {
    private final SearchService searchService;
    private final HybridSearchService hybridSearchService;

    public SearchController(SearchService searchService, HybridSearchService hybridSearchService) {
        this.searchService = searchService;
        this.hybridSearchService = hybridSearchService;
    }

    public List<Note> filterNotes(List<Note> notes, String keyword, String category) {
        return searchService.filterNotes(notes, keyword, category);
    }

    public void searchHybrid(String userId, List<Note> notes, String keyword, String category,
                             HybridSearchService.HybridCallback callback) {
        hybridSearchService.searchHybridAsync(userId, notes, keyword, category, callback);
    }
}
