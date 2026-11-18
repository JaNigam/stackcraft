package dev.stackcraft.api.content.domain;

/** Spaced-repetition card. Its id is unique within the chapter; globally it is {@code chapterId/id}. */
public record Flashcard(String id, String front, String back) {
}
