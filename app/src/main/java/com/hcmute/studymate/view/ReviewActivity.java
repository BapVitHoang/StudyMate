package com.hcmute.studymate.view;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.hcmute.studymate.R;
import com.hcmute.studymate.controller.AuthController;
import com.hcmute.studymate.controller.QuizController;
import com.hcmute.studymate.model.ReviewItem;
import com.hcmute.studymate.service.SrsService;
import com.hcmute.studymate.utils.AppContainer;
import com.hcmute.studymate.utils.ListCallback;
import com.hcmute.studymate.utils.OperationCallback;

import java.util.ArrayList;
import java.util.List;

public class ReviewActivity extends AppCompatActivity {
    private QuizController quizController;
    private TextView emptyText;
    private TextView stemText;
    private TextView answerText;
    private View qualityRow;
    private List<ReviewItem> dueItems = new ArrayList<>();
    private int index;
    private String userId;

    public static Intent newIntent(Context context) {
        return new Intent(context, ReviewActivity.class);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AuthController authController = AppContainer.authController();
        userId = authController.getCurrentUserId();
        if (userId == null) {
            finish();
            return;
        }
        setContentView(R.layout.activity_review);
        quizController = AppContainer.quizController();
        emptyText = findViewById(R.id.reviewEmptyText);
        stemText = findViewById(R.id.reviewStemText);
        answerText = findViewById(R.id.reviewAnswerText);
        qualityRow = findViewById(R.id.reviewQualityRow);
        findViewById(R.id.reviewRevealButton).setOnClickListener(view -> reveal());
        findViewById(R.id.reviewAgainButton).setOnClickListener(view -> rate(SrsService.QUALITY_AGAIN));
        findViewById(R.id.reviewHardButton).setOnClickListener(view -> rate(SrsService.QUALITY_HARD));
        findViewById(R.id.reviewGoodButton).setOnClickListener(view -> rate(SrsService.QUALITY_GOOD));
        findViewById(R.id.reviewEasyButton).setOnClickListener(view -> rate(SrsService.QUALITY_EASY));
        View backButton = findViewById(R.id.reviewBackButton);
        if (backButton != null) {
            backButton.setOnClickListener(view -> finish());
        }
        loadDue();
    }

    private void loadDue() {
        quizController.loadDueReviews(userId, new ListCallback<ReviewItem>() {
            @Override
            public void onSuccess(List<ReviewItem> items) {
                runOnUiThread(() -> {
                    dueItems = items == null ? new ArrayList<>() : items;
                    index = 0;
                    showCurrent();
                });
            }

            @Override
            public void onError(Exception exception) {
                runOnUiThread(() -> Toast.makeText(ReviewActivity.this,
                        exception == null ? "Could not load reviews" : exception.getMessage(),
                        Toast.LENGTH_LONG).show());
            }
        });
    }

    private void showCurrent() {
        answerText.setVisibility(View.GONE);
        qualityRow.setVisibility(View.GONE);
        if (dueItems.isEmpty() || index >= dueItems.size()) {
            emptyText.setVisibility(View.VISIBLE);
            stemText.setText("");
            return;
        }
        emptyText.setVisibility(View.GONE);
        ReviewItem item = dueItems.get(index);
        stemText.setText(item.getStem());
    }

    private void reveal() {
        if (dueItems.isEmpty() || index >= dueItems.size()) {
            return;
        }
        ReviewItem item = dueItems.get(index);
        String explanation = item.getExplanation() == null ? "" : item.getExplanation();
        answerText.setText(item.getAnswer() + (explanation.isEmpty() ? "" : "\n" + explanation));
        answerText.setVisibility(View.VISIBLE);
        qualityRow.setVisibility(View.VISIBLE);
    }

    private void rate(int quality) {
        if (dueItems.isEmpty() || index >= dueItems.size()) {
            return;
        }
        ReviewItem item = dueItems.get(index);
        quizController.reviewCard(userId, item, quality, new OperationCallback() {
            @Override
            public void onSuccess() {
                runOnUiThread(() -> {
                    index++;
                    showCurrent();
                });
            }

            @Override
            public void onError(Exception exception) {
                runOnUiThread(() -> Toast.makeText(ReviewActivity.this,
                        exception == null ? "Could not save review" : exception.getMessage(),
                        Toast.LENGTH_LONG).show());
            }
        });
    }
}
