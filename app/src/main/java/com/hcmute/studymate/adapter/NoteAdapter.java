package com.hcmute.studymate.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.hcmute.studymate.R;
import com.hcmute.studymate.model.Note;
import com.hcmute.studymate.utils.DateTimeUtils;
import com.hcmute.studymate.utils.TagUtils;

import java.util.ArrayList;
import java.util.List;

public class NoteAdapter extends RecyclerView.Adapter<NoteAdapter.NoteViewHolder> {
    public interface OnNoteClickListener {
        void onNoteClick(Note note);
    }

    private final List<Note> notes = new ArrayList<>();
    private final OnNoteClickListener listener;

    public NoteAdapter(OnNoteClickListener listener) {
        this.listener = listener;
    }

    public void submitList(List<Note> newNotes) {
        notes.clear();
        if (newNotes != null) {
            notes.addAll(newNotes);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public NoteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_note, parent, false);
        return new NoteViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NoteViewHolder holder, int position) {
        Note note = notes.get(position);
        holder.titleText.setText(note.getTitle());
        holder.categoryText.setText(note.getCategory());
        holder.previewText.setText(note.getContent());
        holder.updatedAtText.setText("Updated " + DateTimeUtils.formatDateTime(note.getUpdatedAt()));

        String tags = TagUtils.join(note.getTags());
        holder.tagsText.setVisibility(tags.isEmpty() ? View.GONE : View.VISIBLE);
        holder.tagsText.setText(tags.isEmpty() ? "" : "#" + tags.replace(", ", " #"));

        if (note.getReminderAt() == null) {
            holder.reminderText.setVisibility(View.GONE);
        } else {
            holder.reminderText.setVisibility(View.VISIBLE);
            holder.reminderText.setText("Reminder " + DateTimeUtils.formatDateTime(note.getReminderAt()));
        }

        holder.itemView.setOnClickListener(view -> listener.onNoteClick(note));
    }

    @Override
    public int getItemCount() {
        return notes.size();
    }

    static class NoteViewHolder extends RecyclerView.ViewHolder {
        private final TextView titleText;
        private final TextView categoryText;
        private final TextView previewText;
        private final TextView updatedAtText;
        private final TextView tagsText;
        private final TextView reminderText;

        NoteViewHolder(@NonNull View itemView) {
            super(itemView);
            titleText = itemView.findViewById(R.id.itemNoteTitle);
            categoryText = itemView.findViewById(R.id.itemNoteCategory);
            previewText = itemView.findViewById(R.id.itemNotePreview);
            updatedAtText = itemView.findViewById(R.id.itemNoteUpdatedAt);
            tagsText = itemView.findViewById(R.id.itemNoteTags);
            reminderText = itemView.findViewById(R.id.itemNoteReminder);
        }
    }
}
