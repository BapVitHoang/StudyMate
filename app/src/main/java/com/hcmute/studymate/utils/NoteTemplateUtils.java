package com.hcmute.studymate.utils;

public final class NoteTemplateUtils {
    public static final String[] TEMPLATE_NAMES = {
            "Lecture Note",
            "Exam Review",
            "Assignment Plan",
            "Reading Summary"
    };

    private NoteTemplateUtils() {
    }

    public static String contentFor(String templateName) {
        if ("Exam Review".equals(templateName)) {
            return "Exam scope:\n\nImportant formulas / concepts:\n\nCommon mistakes:\n\nPractice questions:\n\nReview plan:";
        }
        if ("Assignment Plan".equals(templateName)) {
            return "Requirement:\n\nApproach:\n\nResources:\n\nProgress notes:\n\nQuestions to ask:";
        }
        if ("Reading Summary".equals(templateName)) {
            return "Source:\n\nMain idea:\n\nKey details:\n\nExamples:\n\nPersonal takeaway:";
        }
        return "Topic:\n\nKey points:\n\nExamples:\n\nQuestions:\n\nReview plan:";
    }

    public static String checklistFor(String templateName) {
        if ("Exam Review".equals(templateName)) {
            return "[ ] Review theory\n[ ] Solve practice problems\n[ ] Mark weak topics\n[ ] Summarize final notes";
        }
        if ("Assignment Plan".equals(templateName)) {
            return "[ ] Read requirements\n[ ] Break into tasks\n[ ] Implement solution\n[ ] Test and polish\n[ ] Submit";
        }
        if ("Reading Summary".equals(templateName)) {
            return "[ ] Read once\n[ ] Highlight key ideas\n[ ] Write summary\n[ ] Add questions";
        }
        return "[ ] Capture lecture points\n[ ] Add examples\n[ ] Write questions\n[ ] Schedule review";
    }
}
