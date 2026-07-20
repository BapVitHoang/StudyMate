package com.hcmute.studymate.service;

/**
 * SM-2 spaced repetition scheduler.
 * Quality: 0-5 (0-2 fail / again, 3 hard, 4 good, 5 easy).
 */
public class SrsService {
    public static final int QUALITY_AGAIN = 1;
    public static final int QUALITY_HARD = 3;
    public static final int QUALITY_GOOD = 4;
    public static final int QUALITY_EASY = 5;

    public void applyReview(com.hcmute.studymate.model.ReviewItem item, int quality, long nowMs) {
        if (item == null) {
            return;
        }
        int q = Math.max(0, Math.min(5, quality));
        double ease = item.getEaseFactor() <= 0 ? 2.5 : item.getEaseFactor();
        ease = ease + (0.1 - (5 - q) * (0.08 + (5 - q) * 0.02));
        if (ease < 1.3) {
            ease = 1.3;
        }
        item.setEaseFactor(ease);

        int repetitions = item.getRepetitions();
        int intervalDays;
        if (q < 3) {
            repetitions = 0;
            intervalDays = 1;
        } else {
            repetitions += 1;
            if (repetitions == 1) {
                intervalDays = 1;
            } else if (repetitions == 2) {
                intervalDays = 6;
            } else {
                intervalDays = Math.max(1, (int) Math.round(item.getIntervalDays() * ease));
            }
        }
        item.setRepetitions(repetitions);
        item.setIntervalDays(intervalDays);
        item.setDueAt(nowMs + intervalDays * 24L * 60L * 60L * 1000L);
        item.setUpdatedAt(nowMs);
    }

    public com.hcmute.studymate.model.ReviewItem fromQuestion(
            com.hcmute.studymate.model.QuizQuestion question, long nowMs) {
        com.hcmute.studymate.model.ReviewItem item = new com.hcmute.studymate.model.ReviewItem();
        item.setQuestionId(question.getId());
        item.setStem(question.getStem());
        item.setAnswer(question.getAnswer());
        item.setExplanation(question.getExplanation());
        item.setSourceNoteId(question.getSourceNoteId());
        item.setType(question.getType());
        item.setEaseFactor(2.5);
        item.setIntervalDays(0);
        item.setRepetitions(0);
        item.setDueAt(nowMs);
        item.setUpdatedAt(nowMs);
        return item;
    }
}
