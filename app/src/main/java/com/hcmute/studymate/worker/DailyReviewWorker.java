package com.hcmute.studymate.worker;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.hcmute.studymate.model.Note;
import com.hcmute.studymate.utils.NotificationHelper;

import java.util.concurrent.TimeUnit;

public class DailyReviewWorker extends Worker {
    private static final String USERS_COLLECTION = "users";
    private static final String NOTES_COLLECTION = "notes";
    private static final long REVIEW_AFTER_MILLIS = TimeUnit.DAYS.toMillis(2);
    private static final long UPCOMING_REMINDER_WINDOW_MILLIS = TimeUnit.DAYS.toMillis(1);

    public DailyReviewWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            return Result.success();
        }

        try {
            QuerySnapshot snapshot = Tasks.await(
                    FirebaseFirestore.getInstance()
                            .collection(USERS_COLLECTION)
                            .document(user.getUid())
                            .collection(NOTES_COLLECTION)
                            .get()
            );

            ReviewSummary summary = buildSummary(snapshot);
            if (summary.reviewCount > 0) {
                NotificationHelper.showDailyReviewNotification(
                        getApplicationContext(),
                        summary.reviewCount,
                        summary.firstTitle
                );
            }
            return Result.success();
        } catch (Exception exception) {
            return Result.retry();
        }
    }

    private ReviewSummary buildSummary(QuerySnapshot snapshot) {
        long now = System.currentTimeMillis();
        int reviewCount = 0;
        String firstTitle = "";

        for (DocumentSnapshot document : snapshot.getDocuments()) {
            Note note = document.toObject(Note.class);
            if (note == null) {
                continue;
            }
            boolean hasUpcomingReminder = note.getReminderAt() != null
                    && note.getReminderAt() >= now
                    && note.getReminderAt() <= now + UPCOMING_REMINDER_WINDOW_MILLIS;
            boolean staleNote = note.getUpdatedAt() > 0L
                    && note.getUpdatedAt() <= now - REVIEW_AFTER_MILLIS;

            if (hasUpcomingReminder || staleNote) {
                reviewCount++;
                if (firstTitle.isEmpty() && note.getTitle() != null) {
                    firstTitle = note.getTitle();
                }
            }
        }

        return new ReviewSummary(reviewCount, firstTitle);
    }

    private static class ReviewSummary {
        private final int reviewCount;
        private final String firstTitle;

        private ReviewSummary(int reviewCount, String firstTitle) {
            this.reviewCount = reviewCount;
            this.firstTitle = firstTitle;
        }
    }
}
