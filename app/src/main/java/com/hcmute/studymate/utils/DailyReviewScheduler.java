package com.hcmute.studymate.utils;

import android.content.Context;

import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.hcmute.studymate.worker.DailyReviewWorker;

import java.util.concurrent.TimeUnit;

public final class DailyReviewScheduler {
    private static final String DAILY_REVIEW_WORK = "daily_review_work";

    private DailyReviewScheduler() {
    }

    public static void schedule(Context context) {
        if (context == null) {
            return;
        }

        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        PeriodicWorkRequest request = new PeriodicWorkRequest.Builder(
                DailyReviewWorker.class,
                1,
                TimeUnit.DAYS
        )
                .setConstraints(constraints)
                .build();

        WorkManager.getInstance(context.getApplicationContext())
                .enqueueUniquePeriodicWork(
                        DAILY_REVIEW_WORK,
                        ExistingPeriodicWorkPolicy.UPDATE,
                        request
                );
    }
}
