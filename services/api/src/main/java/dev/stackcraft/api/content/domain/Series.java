package dev.stackcraft.api.content.domain;

import java.util.List;

/** A learning series (Java Core, DSA, LLD...). Units hold chapter ids in reading order. */
public record Series(String id, String title, String tagline, List<Unit> units) {
    public Series {
        units = List.copyOf(units);
    }

    public record Unit(String id, int number, String title, String summary, List<String> chapterIds) {
        public Unit {
            chapterIds = List.copyOf(chapterIds);
        }
    }
}
