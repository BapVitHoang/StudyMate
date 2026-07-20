package com.hcmute.studymate.ai;

import com.hcmute.studymate.model.RetrievedChunk;

import java.util.List;

final class GeminiPromptBuilder {
    private GeminiPromptBuilder() {
    }

    static String summarize(String locale, String title, String category, String content) {
        return String.join("\n",
                "You are StudyMate, an academic study-note assistant.",
                "Summarize the note for a university student.",
                "Respond with ONLY valid JSON matching this schema:",
                "{",
                "  \"summaryText\": string,",
                "  \"bullets\": string[],",
                "  \"keyTerms\": string[],",
                "  \"confidence\": number",
                "}",
                "Rules:",
                "- Do not invent facts that are not supported by the note.",
                "- Keep bullets short and study-friendly.",
                "- Prefer the student's language when possible; locale hint: " + locale,
                "",
                "Title: " + nullToEmpty(title),
                "Category: " + nullToEmpty(category),
                "Note content:",
                GeminiApiClient.truncate(nullToEmpty(content), 12000)
        );
    }

    static String groundedAnswer(String locale, String question, List<RetrievedChunk> passages) {
        return String.join("\n",
                "You are StudyMate, a grounded study tutor.",
                "Answer the student using ONLY the provided passages.",
                "If the passages are insufficient, say you cannot find that in the notes.",
                "Respond with ONLY valid JSON:",
                "{",
                "  \"answer\": string,",
                "  \"citations\": [{\"noteId\": string, \"title\": string, \"excerpt\": string}]",
                "}",
                "Rules:",
                "- Every factual claim must be supported by a passage.",
                "- citations.noteId must come from the passages.",
                "- Prefer the student's language; locale hint: " + locale,
                "",
                "Question:",
                question,
                "",
                "Passages:",
                formatPassages(passages)
        );
    }

    static String tutor(String locale, String historyBlock, List<RetrievedChunk> passages) {
        return String.join("\n",
                "You are StudyMate, a multi-turn grounded study tutor.",
                "Continue the conversation using ONLY the provided passages plus chat history.",
                "Do not invent facts outside the passages.",
                "Respond with ONLY valid JSON:",
                "{",
                "  \"answer\": string,",
                "  \"citations\": [{\"noteId\": string, \"title\": string, \"excerpt\": string}],",
                "  \"suggestedFollowUps\": string[]",
                "}",
                "Prefer the student's language; locale hint: " + locale,
                "",
                "Chat history:",
                historyBlock,
                "",
                "Passages:",
                formatPassages(passages)
        );
    }

    static String examPrep(String locale, String mode, String topic, List<RetrievedChunk> passages) {
        return String.join("\n",
                "You are StudyMate, an exam-prep synthesizer.",
                "Create a " + mode + " from ONLY the provided passages.",
                "Respond with ONLY valid JSON:",
                "{",
                "  \"title\": string,",
                "  \"sections\": [{\"heading\": string, \"bullets\": string[]}],",
                "  \"citations\": [{\"noteId\": string, \"title\": string, \"excerpt\": string}],",
                "  \"coverageNoteIds\": string[]",
                "}",
                "Rules:",
                "- 3-8 sections, study-friendly bullets.",
                "- coverageNoteIds and citation noteIds must come from passages.",
                "- Prefer student language; locale: " + locale,
                "- Topic focus (may be empty): " + (topic == null || topic.isEmpty() ? "(general)" : topic),
                "",
                "Passages:",
                formatPassages(passages)
        );
    }

    static String quiz(String locale, int questionCount, String typesCsv, List<RetrievedChunk> passages) {
        return String.join("\n",
                "You are StudyMate, a grounded quiz generator.",
                "Create " + questionCount + " study questions from ONLY the passages.",
                "Allowed types: " + typesCsv,
                "Respond with ONLY valid JSON:",
                "{",
                "  \"questions\": [{",
                "    \"type\": \"mcq\" | \"short\",",
                "    \"stem\": string,",
                "    \"choices\": string[],",
                "    \"answer\": string,",
                "    \"explanation\": string,",
                "    \"sourceNoteId\": string,",
                "    \"sourceChunkId\": string",
                "  }]",
                "}",
                "Rules:",
                "- Every question must cite a sourceNoteId from passages.",
                "- For mcq provide exactly 4 choices and one correct answer.",
                "- Prefer student language; locale: " + locale,
                "",
                "Passages:",
                formatPassages(passages)
        );
    }

    static String concepts(String locale, String noteId, String title, String content) {
        return String.join("\n",
                "You are StudyMate, a knowledge-graph builder for study notes.",
                "Extract concepts and relations from the note.",
                "Respond with ONLY valid JSON:",
                "{",
                "  \"concepts\": [{\"name\": string, \"definition\": string, \"importance\": number}],",
                "  \"edges\": [{\"from\": string, \"to\": string, \"relation\": \"relatedTo\"|\"prerequisiteOf\"}]",
                "}",
                "Rules:",
                "- 3-12 concepts, short definitions grounded in the note.",
                "- importance from 0 to 1.",
                "- Prefer student language; locale: " + locale,
                "",
                "noteId: " + nullToEmpty(noteId),
                "Title: " + nullToEmpty(title),
                "Content:",
                GeminiApiClient.truncate(nullToEmpty(content), 12000)
        );
    }

    static String formatPassages(List<RetrievedChunk> passages) {
        if (passages == null || passages.isEmpty()) {
            return "(none)";
        }
        StringBuilder builder = new StringBuilder();
        int count = 0;
        for (RetrievedChunk passage : passages) {
            if (passage == null || passage.getChunk() == null) {
                continue;
            }
            count++;
            builder.append("[P").append(count).append("]\n");
            builder.append("noteId: ").append(nullToEmpty(passage.getChunk().getNoteId())).append('\n');
            builder.append("chunkId: ").append(nullToEmpty(passage.getChunk().getId())).append('\n');
            builder.append("title: ").append(nullToEmpty(passage.getChunk().getTitle())).append('\n');
            builder.append(GeminiApiClient.truncate(nullToEmpty(passage.getChunk().getText()), 2000));
            builder.append("\n\n");
            if (count >= 12) {
                break;
            }
        }
        return builder.toString().trim();
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
