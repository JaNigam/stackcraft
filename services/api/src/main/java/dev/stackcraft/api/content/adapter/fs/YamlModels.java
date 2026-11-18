package dev.stackcraft.api.content.adapter.fs;

import java.util.List;

/**
 * File-format records: the shape of the YAML on disk. Kept apart from the domain so the file format can change
 * (or gain a v2) without touching domain code. Missing fields arrive as {@code null}; the factory applies defaults.
 */
final class YamlModels {
    private YamlModels() {}

    record SeriesFile(String id, String title, String tagline, List<UnitFile> units) {}

    record UnitFile(String id, Integer number, String dir, String title, String summary, List<String> chapters) {}

    record ChapterFile(String id, Integer lesson, String title, String subtitle, Integer minutes, Integer completionXp,
                       List<String> whereAsked, List<String> prerequisites, List<String> codeFiles) {}

    record TeasersFile(List<TeaserFile> teasers) {}

    record TeaserFile(String id, String prompt, String code, List<String> options, Integer answer, String reveal,
                      Integer xp) {}

    record QuestionsFile(List<QuestionFile> questions, List<FlashcardFile> flashcards) {}

    /** Union of every question type's fields; {@code type} decides which ones matter. */
    record QuestionFile(String id, String tier, String type, String prompt, String code, List<String> options,
                        Integer answer, String expected, String explanation, String modelAnswer,
                        List<String> keyPoints, List<String> hints, Integer xp) {}

    record FlashcardFile(String id, String front, String back) {}
}
