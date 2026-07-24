package com.hcmute.studymate.view;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.hcmute.studymate.R;
import com.hcmute.studymate.controller.AuthController;
import com.hcmute.studymate.controller.TutorController;
import com.hcmute.studymate.model.RagCitation;
import com.hcmute.studymate.model.TutorMessage;
import com.hcmute.studymate.model.TutorReply;
import com.hcmute.studymate.model.TutorSession;
import com.hcmute.studymate.utils.AppContainer;
import com.hcmute.studymate.utils.DataCallback;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class TutorActivity extends AppCompatActivity {
    private AuthController authController;
    private TutorController tutorController;
    private TutorSession session;
    private MessageAdapter adapter;
    private TextInputEditText tutorInput;
    private ChipGroup followUpChipGroup;
    private String userId;
    private boolean sending;

    public static Intent newIntent(Context context) {
        return new Intent(context, TutorActivity.class);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        authController = AppContainer.authController();
        userId = authController.getCurrentUserId();
        if (userId == null) {
            finish();
            return;
        }
        setContentView(R.layout.activity_tutor);
        tutorController = AppContainer.tutorController();
        session = new TutorSession();
        session.setMessages(new ArrayList<>());

        RecyclerView recyclerView = findViewById(R.id.tutorMessagesRecycler);
        tutorInput = findViewById(R.id.tutorInput);
        // Material DayNight can force light text onto our light input box — lock colors.
        tutorInput.setTextColor(ContextCompat.getColor(this, R.color.studymate_text_primary));
        tutorInput.setHintTextColor(ContextCompat.getColor(this, R.color.studymate_text_secondary));
        followUpChipGroup = findViewById(R.id.followUpChipGroup);
        MaterialButton sendButton = findViewById(R.id.tutorSendButton);
        View backButton = findViewById(R.id.tutorBackButton);
        if (backButton != null) {
            backButton.setOnClickListener(view -> finish());
        }

        adapter = new MessageAdapter();
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(adapter);
        sendButton.setOnClickListener(view -> sendCurrentInput());
    }

    private void sendCurrentInput() {
        if (sending) {
            return;
        }
        String text = tutorInput.getText() == null ? "" : tutorInput.getText().toString().trim();
        if (text.isEmpty()) {
            return;
        }
        sending = true;
        tutorInput.setText("");
        adapter.submit(session.getMessages());
        scrollToBottom();
        tutorController.sendMessage(userId, session, text, Locale.getDefault().toLanguageTag(),
                new DataCallback<TutorReply>() {
                    @Override
                    public void onSuccess(TutorReply data) {
                        runOnUiThread(() -> {
                            sending = false;
                            adapter.submit(session.getMessages());
                            scrollToBottom();
                            renderFollowUps(data);
                            if (data.getCitations() != null) {
                                for (RagCitation citation : data.getCitations()) {
                                }
                            }
                            if (data.isUsedFallback()) {
                                Toast.makeText(TutorActivity.this, R.string.tutor_offline_fallback,
                                        Toast.LENGTH_SHORT).show();
                            }
                        });
                    }

                    @Override
                    public void onError(Exception exception) {
                        runOnUiThread(() -> {
                            sending = false;
                            Toast.makeText(TutorActivity.this,
                                    exception == null ? "Tutor failed" : exception.getMessage(),
                                    Toast.LENGTH_LONG).show();
                        });
                    }
                });
        adapter.submit(session.getMessages());
        scrollToBottom();
    }

    private void scrollToBottom() {
        RecyclerView recyclerView = findViewById(R.id.tutorMessagesRecycler);
        if (recyclerView != null && adapter != null && adapter.getItemCount() > 0) {
            recyclerView.smoothScrollToPosition(adapter.getItemCount() - 1);
        }
    }

    private void renderFollowUps(TutorReply reply) {
        followUpChipGroup.removeAllViews();
        if (reply.getSuggestedFollowUps() == null) {
            return;
        }
        for (String followUp : reply.getSuggestedFollowUps()) {
            Chip chip = new Chip(this);
            chip.setText(followUp);
            chip.setOnClickListener(view -> {
                tutorInput.setText(followUp);
                sendCurrentInput();
            });
            followUpChipGroup.addView(chip);
        }
        if (reply.getCitations() != null) {
            for (RagCitation citation : reply.getCitations()) {
                Chip chip = new Chip(this);
                String title = citation.getTitle() == null || citation.getTitle().isEmpty()
                        ? getString(R.string.rag_citations) : citation.getTitle();
                chip.setText(title);
                chip.setOnClickListener(view -> {
                    if (citation.getNoteId() != null) {
                        startActivity(NoteDetailActivity.newIntent(this, citation.getNoteId()));
                    }
                });
                followUpChipGroup.addView(chip);
            }
        }
    }

    private static final class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.Holder> {
        private final List<TutorMessage> items = new ArrayList<>();

        void submit(List<TutorMessage> messages) {
            items.clear();
            if (messages != null) {
                items.addAll(messages);
            }
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_tutor_message, parent, false);
            return new Holder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull Holder holder, int position) {
            TutorMessage message = items.get(position);
            boolean isUser = TutorMessage.ROLE_USER.equals(message.getRole());
            if (isUser) {
                holder.userContainer.setVisibility(View.VISIBLE);
                holder.tutorContainer.setVisibility(View.GONE);
                holder.userText.setText(message.getContent());
            } else {
                holder.tutorContainer.setVisibility(View.VISIBLE);
                holder.userContainer.setVisibility(View.GONE);
                holder.tutorText.setText(message.getContent());
            }
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        static final class Holder extends RecyclerView.ViewHolder {
            private final View userContainer;
            private final View tutorContainer;
            private final TextView userText;
            private final TextView tutorText;

            Holder(View itemView) {
                super(itemView);
                userContainer = itemView.findViewById(R.id.userMessageContainer);
                tutorContainer = itemView.findViewById(R.id.tutorMessageContainer);
                userText = itemView.findViewById(R.id.userMessageText);
                tutorText = itemView.findViewById(R.id.tutorMessageText);
            }
        }
    }
}
