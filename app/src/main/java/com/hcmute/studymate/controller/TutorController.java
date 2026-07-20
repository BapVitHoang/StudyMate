package com.hcmute.studymate.controller;

import com.hcmute.studymate.model.TutorReply;
import com.hcmute.studymate.model.TutorSession;
import com.hcmute.studymate.service.TutorService;
import com.hcmute.studymate.utils.DataCallback;
import com.hcmute.studymate.utils.OperationCallback;

public class TutorController {
    private final TutorService tutorService;

    public TutorController(TutorService tutorService) {
        this.tutorService = tutorService;
    }

    public void sendMessage(String userId, TutorSession session, String userText, String locale,
                            DataCallback<TutorReply> callback) {
        tutorService.sendMessage(userId, session, userText, locale, callback);
    }

    public void saveSession(String userId, TutorSession session, OperationCallback callback) {
        tutorService.saveSession(userId, session, callback);
    }
}
