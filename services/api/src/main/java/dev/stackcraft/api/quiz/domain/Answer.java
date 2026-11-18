package dev.stackcraft.api.quiz.domain;

/**
 * What the learner submitted. One shape for every question type; each grader reads the fields it needs and
 * treats missing ones as a wrong answer (never an exception: that is part of the grader contract).
 *
 * @param choice    MCQ option index
 * @param text      typed output or free text
 * @param selfScore 0, 1 or 2 for self-graded types
 * @param hintsUsed hints revealed before answering (problems)
 */
public record Answer(Integer choice, String text, Integer selfScore, Integer hintsUsed) {

    public static Answer choice(int index) { return new Answer(index, null, null, null); }

    public static Answer text(String text) { return new Answer(null, text, null, null); }

    public static Answer selfScored(int score, int hintsUsed) { return new Answer(null, null, score, hintsUsed); }
}
