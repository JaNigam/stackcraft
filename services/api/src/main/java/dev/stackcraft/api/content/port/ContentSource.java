package dev.stackcraft.api.content.port;

import dev.stackcraft.api.content.domain.Chapter;
import dev.stackcraft.api.content.domain.Series;

import java.util.List;
import java.util.Optional;

/**
 * Read-only port for content (ISP: writing content, if it ever comes, is a separate {@code ContentAdmin} port).
 * Today backed by files in Git; a database or S3 adapter can replace it without touching any use case.
 */
public interface ContentSource {

    List<Series> allSeries();

    Optional<Series> series(String seriesId);

    Optional<Chapter> chapter(String chapterId);

    List<Chapter> allChapters();
}
