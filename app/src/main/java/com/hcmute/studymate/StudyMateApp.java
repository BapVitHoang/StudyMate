package com.hcmute.studymate;

import android.app.Application;

import com.hcmute.studymate.utils.AppContainer;

public class StudyMateApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        AppContainer.init(this);
    }
}
