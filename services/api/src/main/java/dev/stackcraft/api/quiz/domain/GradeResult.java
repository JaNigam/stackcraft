package dev.stackcraft.api.quiz.domain;

/**
 * Outcome of grading.
 *
 * @param correct     fully right
 * @param score       0.0 to 1.0; XP awarded is {@code round(question.xp * score)}
 * @param feedback    one short line for the learner
 * @param explanation the why (MCQ/predict) or the model answer (short/problem)
 */
public record GradeResult(boolean correct, double score, String feedback, String explanation) {
    public GradeResult {
        if (score < 0.0 || score > 1.0 || Double.isNaN(score)) throw new IllegalArgumentException("score must be in [0, 1]");
        feedback = feedback == null ? "" : feedback;
        explanation = explanation == null ? "" : explanation;
    }
}
