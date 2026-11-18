package dev.stackcraft.api.content.application;

import dev.stackcraft.api.content.domain.Series;
import dev.stackcraft.api.content.port.ContentSource;
import dev.stackcraft.api.shared.NotFoundException;

import java.util.List;

public class ListSeriesUseCase {

    private final ContentSource content;

    public ListSeriesUseCase(ContentSource content) {
        this.content = content;
    }

    public List<Series> all() {
        return content.allSeries();
    }

    public Series one(String seriesId) {
        return content.series(seriesId).orElseThrow(() -> new NotFoundException("Series", seriesId));
    }
}
