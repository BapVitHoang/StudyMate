package com.hcmute.studymate.ml;

import com.hcmute.studymate.model.Note;
import com.hcmute.studymate.utils.Constants;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class NoteChunker {
    public List<String> chunk(Note note) {
        if (note == null) {
            return new ArrayList<>();
        }
        String title = note.getTitle() == null ? "" : note.getTitle().trim();
        String content = note.getContent() == null ? "" : note.getContent().trim();
        if (content.isEmpty()) {
            return new ArrayList<>();
        }

        String source = content;
        if (!title.isEmpty()) {
            source = title + "\n\n" + content;
        }

        List<String> paragraphs = splitParagraphs(source);
        List<String> chunks = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String paragraph : paragraphs) {
            if (paragraph.isEmpty()) {
                continue;
            }
            if (paragraph.length() > Constants.CHUNK_TARGET_CHARS) {
                flush(current, chunks);
                chunks.addAll(splitLongParagraph(paragraph));
                continue;
            }
            if (current.length() == 0) {
                current.append(paragraph);
            } else if (current.length() + 1 + paragraph.length() <= Constants.CHUNK_TARGET_CHARS) {
                current.append('\n').append(paragraph);
            } else {
                flush(current, chunks);
                String overlap = tailOverlap(chunks.isEmpty() ? "" : chunks.get(chunks.size() - 1));
                if (!overlap.isEmpty()) {
                    current.append(overlap).append('\n');
                }
                current.append(paragraph);
            }
            if (chunks.size() >= Constants.MAX_CHUNKS_PER_NOTE) {
                break;
            }
        }
        flush(current, chunks);

        if (chunks.size() > Constants.MAX_CHUNKS_PER_NOTE) {
            return new ArrayList<>(chunks.subList(0, Constants.MAX_CHUNKS_PER_NOTE));
        }
        return chunks;
    }

    private void flush(StringBuilder current, List<String> chunks) {
        if (current.length() == 0) {
            return;
        }
        chunks.add(current.toString().trim());
        current.setLength(0);
    }

    private List<String> splitParagraphs(String source) {
        String[] raw = source.split("\\n\\s*\\n|\\n");
        List<String> paragraphs = new ArrayList<>();
        for (String item : raw) {
            String cleaned = item.trim();
            if (!cleaned.isEmpty()) {
                paragraphs.add(cleaned);
            }
        }
        if (paragraphs.isEmpty() && source.trim().length() > 0) {
            paragraphs.add(source.trim());
        }
        return paragraphs;
    }

    private List<String> splitLongParagraph(String paragraph) {
        List<String> parts = new ArrayList<>();
        String[] sentences = paragraph.split("(?<=[.!?])\\s+");
        StringBuilder current = new StringBuilder();
        for (String sentence : sentences) {
            String cleaned = sentence.trim();
            if (cleaned.isEmpty()) {
                continue;
            }
            if (current.length() == 0) {
                current.append(cleaned);
            } else if (current.length() + 1 + cleaned.length() <= Constants.CHUNK_TARGET_CHARS) {
                current.append(' ').append(cleaned);
            } else {
                parts.add(current.toString().trim());
                String overlap = tailOverlap(parts.get(parts.size() - 1));
                current.setLength(0);
                if (!overlap.isEmpty()) {
                    current.append(overlap).append(' ');
                }
                current.append(cleaned);
            }
            if (parts.size() >= Constants.MAX_CHUNKS_PER_NOTE) {
                break;
            }
        }
        if (current.length() > 0 && parts.size() < Constants.MAX_CHUNKS_PER_NOTE) {
            parts.add(current.toString().trim());
        }
        if (parts.isEmpty()) {
            parts.add(paragraph.substring(0, Math.min(Constants.CHUNK_TARGET_CHARS, paragraph.length())));
        }
        return parts;
    }

    private String tailOverlap(String text) {
        if (text == null || text.isEmpty() || Constants.CHUNK_OVERLAP_CHARS <= 0) {
            return "";
        }
        int start = Math.max(0, text.length() - Constants.CHUNK_OVERLAP_CHARS);
        String overlap = text.substring(start).trim();
        int space = overlap.indexOf(' ');
        if (space > 0 && space < overlap.length() - 1) {
            overlap = overlap.substring(space + 1);
        }
        return overlap.toLowerCase(Locale.US).isEmpty() ? "" : overlap;
    }
}
