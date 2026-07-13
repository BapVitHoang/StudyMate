package com.hcmute.studymate.view;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.hcmute.studymate.R;
import com.hcmute.studymate.controller.AuthController;
import com.hcmute.studymate.controller.StudySessionController;
import com.hcmute.studymate.model.StudySession;
import com.hcmute.studymate.model.StudyStats;
import com.hcmute.studymate.utils.AppContainer;
import com.hcmute.studymate.utils.DataCallback;
import com.hcmute.studymate.utils.DateTimeUtils;
import com.hcmute.studymate.utils.ListCallback;

import java.util.List;
import java.util.Locale;

public class StudyStatsActivity extends AppCompatActivity {
    private AuthController authController;
    private StudySessionController sessionController;
    private TextView totalTimeText;
    private TextView sessionCountText;
    private TextView topCategoryText;
    private TextView latestNoteText;
    private LinearLayout recentSessionsContainer;
    private ProgressBar loadingProgress;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        authController = AppContainer.authController();
        userId = authController.getCurrentUserId();
        if (userId == null || userId.trim().isEmpty()) {
            openLoginAndFinish();
            return;
        }
        setContentView(R.layout.activity_study_stats);
        sessionController = AppContainer.studySessionController();
        totalTimeText = findViewById(R.id.statsTotalTimeText);
        sessionCountText = findViewById(R.id.statsSessionCountText);
        topCategoryText = findViewById(R.id.statsTopCategoryText);
        latestNoteText = findViewById(R.id.statsLatestNoteText);
        recentSessionsContainer = findViewById(R.id.recentSessionsContainer);
        loadingProgress = findViewById(R.id.statsLoadingProgress);
        MaterialButton backButton = findViewById(R.id.statsBackButton);
        backButton.setOnClickListener(view -> finish());
        loadStats();
    }

    private void loadStats() {
        loadingProgress.setVisibility(View.VISIBLE);
        sessionController.loadStats(userId, new DataCallback<StudyStats>() {
            @Override
            public void onSuccess(StudyStats data) {
                renderStats(data);
                loadRecentSessions();
            }

            @Override
            public void onError(Exception exception) {
                loadingProgress.setVisibility(View.GONE);
                showError("Could not load study stats", exception);
            }
        });
    }

    private void loadRecentSessions() {
        sessionController.loadRecentSessions(userId, new ListCallback<StudySession>() {
            @Override
            public void onSuccess(List<StudySession> items) {
                loadingProgress.setVisibility(View.GONE);
                renderRecentSessions(items);
            }

            @Override
            public void onError(Exception exception) {
                loadingProgress.setVisibility(View.GONE);
                showError("Could not load recent sessions", exception);
            }
        });
    }

    private void renderStats(StudyStats stats) {
        totalTimeText.setText(formatMinutes(stats.getTotalMillis()) + " studied");
        sessionCountText.setText(stats.getSessionCount() + " focus sessions");
        topCategoryText.setText("Top category: " + emptyFallback(stats.getTopCategory()));
        latestNoteText.setText("Latest note: " + emptyFallback(stats.getLatestNoteTitle()));
    }

    private void renderRecentSessions(List<StudySession> sessions) {
        recentSessionsContainer.removeAllViews();
        if (sessions == null || sessions.isEmpty()) {
            recentSessionsContainer.addView(buildSessionText("No focus sessions yet. Start one from a note detail screen."));
            return;
        }

        for (StudySession session : sessions) {
            String line = emptyFallback(session.getNoteTitle()) + "\n"
                    + formatMinutes(session.getDurationMillis()) + " - "
                    + DateTimeUtils.formatDateTime(session.getCompletedAt());
            recentSessionsContainer.addView(buildSessionText(line));
        }
    }

    private TextView buildSessionText(String text) {
        TextView view = new TextView(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, getResources().getDimensionPixelSize(R.dimen.space_md));
        view.setLayoutParams(params);
        view.setBackgroundResource(R.drawable.bg_auth_panel);
        view.setPadding(
                getResources().getDimensionPixelSize(R.dimen.space_lg),
                getResources().getDimensionPixelSize(R.dimen.space_md),
                getResources().getDimensionPixelSize(R.dimen.space_lg),
                getResources().getDimensionPixelSize(R.dimen.space_md)
        );
        view.setText(text);
        view.setTextColor(getColor(R.color.studymate_text_primary));
        view.setTextSize(14f);
        view.setLineSpacing(3f, 1f);
        return view;
    }

    private String formatMinutes(long millis) {
        long minutes = Math.max(1L, millis / 60000L);
        return String.format(Locale.US, "%d min", minutes);
    }

    private String emptyFallback(String value) {
        return value == null || value.trim().isEmpty() ? "-" : value;
    }

    private void openLoginAndFinish() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void showError(String prefix, Exception exception) {
        String detail = exception == null || exception.getMessage() == null
                ? "Please check your connection and Firestore rules."
                : exception.getMessage();
        Toast.makeText(this, prefix + ". " + detail, Toast.LENGTH_LONG).show();
    }
}
