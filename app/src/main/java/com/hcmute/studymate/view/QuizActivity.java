package com.hcmute.studymate.view;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.hcmute.studymate.R;
import com.hcmute.studymate.controller.AuthController;
import com.hcmute.studymate.controller.QuizController;
import com.hcmute.studymate.model.QuizQuestion;
import com.hcmute.studymate.utils.AppContainer;
import com.hcmute.studymate.utils.Constants;
import com.hcmute.studymate.utils.DataCallback;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class QuizActivity extends AppCompatActivity {
    private static final String EXTRA_NOTE_ID = "extra_note_id";

    private QuizController quizController;
    private TextInputEditText topicInput;
    private ProgressBar progressBar;
    private TextView stemText;
    private RadioGroup choicesGroup;
    private TextInputEditText shortAnswerInput;
    private TextView feedbackText;
    private List<QuizQuestion> questions = new ArrayList<>();
    private int index;
    private String userId;
    private String scopedNoteId;

    public static Intent newIntent(Context context) {
        return new Intent(context, QuizActivity.class);
    }

    public static Intent newIntentForNote(Context context, String noteId) {
        Intent intent = new Intent(context, QuizActivity.class);
        intent.putExtra(EXTRA_NOTE_ID, noteId);
        return intent;
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
        scopedNoteId = getIntent().getStringExtra(EXTRA_NOTE_ID);
        setContentView(R.layout.activity_quiz);
        quizController = AppContainer.quizController();
        topicInput = findViewById(R.id.quizTopicInput);
        progressBar = findViewById(R.id.quizProgress);
        stemText = findViewById(R.id.quizStemText);
        choicesGroup = findViewById(R.id.quizChoicesGroup);
        shortAnswerInput = findViewById(R.id.quizShortAnswerInput);
        feedbackText = findViewById(R.id.quizFeedbackText);
        findViewById(R.id.generateQuizButton).setOnClickListener(view -> generate());
        findViewById(R.id.quizCheckButton).setOnClickListener(view -> checkAnswer());
        findViewById(R.id.quizNextButton).setOnClickListener(view -> nextQuestion());
        View backButton = findViewById(R.id.quizBackButton);
        if (backButton != null) {
            backButton.setOnClickListener(view -> finish());
        }
    }

    private void generate() {
        progressBar.setVisibility(View.VISIBLE);
        String topic = topicInput.getText() == null ? "" : topicInput.getText().toString().trim();
        List<String> noteIds = null;
        if (scopedNoteId != null && !scopedNoteId.isEmpty()) {
            noteIds = new ArrayList<>();
            noteIds.add(scopedNoteId);
        }
        quizController.generateQuiz(userId, topic, noteIds, Constants.DEFAULT_QUIZ_COUNT,
                Locale.getDefault().toLanguageTag(), new DataCallback<List<QuizQuestion>>() {
                    @Override
                    public void onSuccess(List<QuizQuestion> data) {
                        runOnUiThread(() -> {
                            progressBar.setVisibility(View.GONE);
                            questions = data == null ? new ArrayList<>() : data;
                            index = 0;
                            showCurrent();
                        });
                    }

                    @Override
                    public void onError(Exception exception) {
                        runOnUiThread(() -> {
                            progressBar.setVisibility(View.GONE);
                            Toast.makeText(QuizActivity.this,
                                    exception == null ? "Quiz failed" : exception.getMessage(),
                                    Toast.LENGTH_LONG).show();
                        });
                    }
                });
    }

    private void showCurrent() {
        feedbackText.setText("");
        if (questions.isEmpty() || index >= questions.size()) {
            stemText.setText(R.string.quiz_done);
            choicesGroup.removeAllViews();
            shortAnswerInput.setVisibility(View.GONE);
            return;
        }
        QuizQuestion question = questions.get(index);
        stemText.setText((index + 1) + "/" + questions.size() + ". " + question.getStem());
        choicesGroup.removeAllViews();
        if (QuizQuestion.TYPE_MCQ.equals(question.getType())
                && question.getChoices() != null && !question.getChoices().isEmpty()) {
            shortAnswerInput.setVisibility(View.GONE);
            choicesGroup.setVisibility(View.VISIBLE);
            for (String choice : question.getChoices()) {
                RadioButton button = new RadioButton(this);
                button.setText(choice);
                choicesGroup.addView(button);
            }
        } else {
            choicesGroup.setVisibility(View.GONE);
            shortAnswerInput.setVisibility(View.VISIBLE);
            shortAnswerInput.setText("");
        }
    }

    private void checkAnswer() {
        if (questions.isEmpty() || index >= questions.size()) {
            return;
        }
        QuizQuestion question = questions.get(index);
        String expected = question.getAnswer() == null ? "" : question.getAnswer().trim();
        String actual;
        if (QuizQuestion.TYPE_MCQ.equals(question.getType())) {
            int checked = choicesGroup.getCheckedRadioButtonId();
            if (checked == -1) {
                feedbackText.setText(R.string.quiz_pick_choice);
                return;
            }
            RadioButton selected = findViewById(checked);
            actual = selected.getText().toString().trim();
        } else {
            actual = shortAnswerInput.getText() == null ? "" : shortAnswerInput.getText().toString().trim();
        }
        boolean correct = actual.equalsIgnoreCase(expected)
                || expected.toLowerCase(Locale.US).contains(actual.toLowerCase(Locale.US));
        String explanation = question.getExplanation() == null ? "" : question.getExplanation();
        feedbackText.setText((correct ? getString(R.string.quiz_correct) : getString(R.string.quiz_incorrect))
                + "\n" + getString(R.string.quiz_answer_label, expected)
                + (explanation.isEmpty() ? "" : "\n" + explanation));
    }

    private void nextQuestion() {
        index++;
        showCurrent();
    }
}
