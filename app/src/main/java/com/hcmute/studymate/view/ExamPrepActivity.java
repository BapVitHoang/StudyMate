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

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.hcmute.studymate.R;
import com.hcmute.studymate.controller.AuthController;
import com.hcmute.studymate.controller.ExamPrepController;
import com.hcmute.studymate.model.ExamPrepResult;
import com.hcmute.studymate.model.ExamPrepSection;
import com.hcmute.studymate.model.RagCitation;
import com.hcmute.studymate.utils.AppContainer;
import com.hcmute.studymate.utils.DataCallback;

import java.util.Locale;

public class ExamPrepActivity extends AppCompatActivity {
    private ExamPrepController examPrepController;
    private TextInputEditText topicInput;
    private RadioGroup modeGroup;
    private ProgressBar progressBar;
    private TextView resultText;
    private android.widget.LinearLayout citationsContainer;
    private String userId;

    public static Intent newIntent(Context context) {
        return new Intent(context, ExamPrepActivity.class);
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
        setContentView(R.layout.activity_exam_prep);
        examPrepController = AppContainer.examPrepController();
        topicInput = findViewById(R.id.examTopicInput);
        modeGroup = findViewById(R.id.examModeGroup);
        progressBar = findViewById(R.id.examProgress);
        resultText = findViewById(R.id.examResultText);
        citationsContainer = findViewById(R.id.examCitationsContainer);
        findViewById(R.id.generateExamButton).setOnClickListener(view -> generate());
    }

    private void generate() {
        String topic = topicInput.getText() == null ? "" : topicInput.getText().toString().trim();
        String mode = "outline";
        int checked = modeGroup.getCheckedRadioButtonId();
        if (checked == R.id.modeCheatsheet) {
            mode = "cheatsheet";
        } else if (checked == R.id.modePractice) {
            mode = "practice_outline";
        }
        progressBar.setVisibility(View.VISIBLE);
        examPrepController.synthesize(userId, mode, topic, null,
                Locale.getDefault().toLanguageTag(), new DataCallback<ExamPrepResult>() {
                    @Override
                    public void onSuccess(ExamPrepResult data) {
                        runOnUiThread(() -> {
                            progressBar.setVisibility(View.GONE);
                            render(data);
                        });
                    }

                    @Override
                    public void onError(Exception exception) {
                        runOnUiThread(() -> {
                            progressBar.setVisibility(View.GONE);
                            Toast.makeText(ExamPrepActivity.this,
                                    exception == null ? "Exam prep failed" : exception.getMessage(),
                                    Toast.LENGTH_LONG).show();
                        });
                    }
                });
    }

    private void render(ExamPrepResult data) {
        StringBuilder builder = new StringBuilder();
        builder.append(data.getTitle() == null ? getString(R.string.exam_prep_title) : data.getTitle());
        builder.append("\n\n");
        if (data.getSections() != null) {
            for (ExamPrepSection section : data.getSections()) {
                builder.append("• ").append(section.getHeading()).append('\n');
                if (section.getBullets() != null) {
                    for (String bullet : section.getBullets()) {
                        builder.append("   - ").append(bullet).append('\n');
                    }
                }
                builder.append('\n');
            }
        }
        resultText.setText(builder.toString());
        citationsContainer.removeAllViews();
        if (data.getCitations() == null) {
            return;
        }
        for (RagCitation citation : data.getCitations()) {
            MaterialButton button = new MaterialButton(this, null,
                    com.google.android.material.R.attr.materialButtonOutlinedStyle);
            button.setText(citation.getTitle() == null ? citation.getNoteId() : citation.getTitle());
            button.setOnClickListener(view -> {
                if (citation.getNoteId() != null) {
                    startActivity(NoteDetailActivity.newIntent(this, citation.getNoteId()));
                }
            });
            citationsContainer.addView(button);
        }
    }
}
