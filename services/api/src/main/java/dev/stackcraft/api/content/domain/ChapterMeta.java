package dev.stackcraft.api.content.domain;

import java.util.List;

/**
 * chapter.yaml.
 *
 * @param id            globally unique, e.g. {@code java-core.u00.l01}
 * @param whereAsked    real interview contexts ("GS Superday R1", ...)
 * @param completionXp  XP for finishing the lesson (course rule: 10)
 */
public record ChapterMeta(String id, String seriesId, String unitId, int lesson, String title, String subtitle,
                          int minutes, int completionXp, List<String> whereAsked, List<String> prerequisites,
                          List<String> codeFiles) {
    public ChapterMeta {
        whereAsked = List.copyOf(whereAsked);
        prerequisites = List.copyOf(prerequisites);
        codeFiles = List.copyOf(codeFiles);
    }
}
