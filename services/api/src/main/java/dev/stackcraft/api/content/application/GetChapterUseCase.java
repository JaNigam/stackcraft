package dev.stackcraft.api.content.application;

import dev.stackcraft.api.content.domain.Chapter;
import dev.stackcraft.api.content.port.ContentSource;
import dev.stackcraft.api.shared.NotFoundException;

public class GetChapterUseCase {

    private final ContentSource content;

    public GetChapterUseCase(ContentSource content) {
        this.content = content;
    }

    public Chapter get(String chapterId) {
        return content.chapter(chapterId).orElseThrow(() -> new NotFoundException("Chapter", chapterId));
    }
}
