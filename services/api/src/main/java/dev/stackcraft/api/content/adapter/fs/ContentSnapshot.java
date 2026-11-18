package dev.stackcraft.api.content.adapter.fs;

import dev.stackcraft.api.content.domain.Chapter;
import dev.stackcraft.api.content.domain.Series;

import java.util.List;
import java.util.Map;

/** Immutable view of all content at one moment. Swapped atomically on reload, so readers never see half a reload. */
record ContentSnapshot(List<Series> series, Map<String, Chapter> chapters) {
    ContentSnapshot {
        series = List.copyOf(series);
        chapters = Map.copyOf(chapters);
    }
}
