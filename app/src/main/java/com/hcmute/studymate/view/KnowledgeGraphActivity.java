package com.hcmute.studymate.view;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.hcmute.studymate.R;
import com.hcmute.studymate.controller.AuthController;
import com.hcmute.studymate.controller.KnowledgeGraphController;
import com.hcmute.studymate.model.Concept;
import com.hcmute.studymate.model.LearningGap;
import com.hcmute.studymate.utils.AppContainer;
import com.hcmute.studymate.utils.DataCallback;
import com.hcmute.studymate.utils.ListCallback;
import com.hcmute.studymate.utils.OperationCallback;

import java.util.ArrayList;
import java.util.List;

public class KnowledgeGraphActivity extends AppCompatActivity {
    private KnowledgeGraphController knowledgeGraphController;
    private TextView studyNextText;
    private ConceptAdapter adapter;
    private String userId;

    public static Intent newIntent(Context context) {
        return new Intent(context, KnowledgeGraphActivity.class);
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
        setContentView(R.layout.activity_knowledge_graph);
        knowledgeGraphController = AppContainer.knowledgeGraphController();
        studyNextText = findViewById(R.id.studyNextText);
        RecyclerView recyclerView = findViewById(R.id.conceptsRecycler);
        adapter = new ConceptAdapter();
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
        findViewById(R.id.rebuildGapsButton).setOnClickListener(view -> rebuildGaps());
        reload();
    }

    private void reload() {
        knowledgeGraphController.studyNext(userId, new DataCallback<List<LearningGap>>() {
            @Override
            public void onSuccess(List<LearningGap> data) {
                runOnUiThread(() -> {
                    if (data == null || data.isEmpty()) {
                        studyNextText.setText(R.string.study_next_empty);
                        return;
                    }
                    StringBuilder builder = new StringBuilder(getString(R.string.study_next_header));
                    builder.append('\n');
                    for (LearningGap gap : data) {
                        builder.append("• ").append(gap.getConceptName())
                                .append(" — ").append(gap.getReason()).append('\n');
                    }
                    studyNextText.setText(builder.toString());
                });
            }

            @Override
            public void onError(Exception exception) {
                runOnUiThread(() -> studyNextText.setText(R.string.study_next_empty));
            }
        });

        knowledgeGraphController.listConcepts(userId, new ListCallback<Concept>() {
            @Override
            public void onSuccess(List<Concept> items) {
                runOnUiThread(() -> adapter.submit(items));
            }

            @Override
            public void onError(Exception exception) {
                runOnUiThread(() -> Toast.makeText(KnowledgeGraphActivity.this,
                        exception == null ? "Could not load concepts" : exception.getMessage(),
                        Toast.LENGTH_LONG).show());
            }
        });
    }

    private void rebuildGaps() {
        knowledgeGraphController.rebuildGaps(userId, new OperationCallback() {
            @Override
            public void onSuccess() {
                runOnUiThread(() -> {
                    Toast.makeText(KnowledgeGraphActivity.this, R.string.gaps_rebuilt, Toast.LENGTH_SHORT).show();
                    reload();
                });
            }

            @Override
            public void onError(Exception exception) {
                runOnUiThread(() -> Toast.makeText(KnowledgeGraphActivity.this,
                        exception == null ? "Rebuild failed" : exception.getMessage(),
                        Toast.LENGTH_LONG).show());
            }
        });
    }

    private static final class ConceptAdapter extends RecyclerView.Adapter<ConceptAdapter.Holder> {
        private final List<Concept> items = new ArrayList<>();

        void submit(List<Concept> concepts) {
            items.clear();
            if (concepts != null) {
                items.addAll(concepts);
            }
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            TextView textView = (TextView) LayoutInflater.from(parent.getContext())
                    .inflate(android.R.layout.simple_list_item_2, parent, false);
            return new Holder(textView);
        }

        @Override
        public void onBindViewHolder(@NonNull Holder holder, int position) {
            Concept concept = items.get(position);
            holder.title.setText(concept.getName());
            String related = concept.getNoteIds() == null ? "0" : String.valueOf(concept.getNoteIds().size());
            holder.subtitle.setText((concept.getDefinition() == null ? "" : concept.getDefinition())
                    + "\nNotes: " + related);
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        static final class Holder extends RecyclerView.ViewHolder {
            private final TextView title;
            private final TextView subtitle;

            Holder(TextView root) {
                super(root);
                title = root.findViewById(android.R.id.text1);
                subtitle = root.findViewById(android.R.id.text2);
            }
        }
    }
}
