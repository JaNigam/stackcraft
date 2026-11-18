package dev.stackcraft.api.content.adapter.fs;

import dev.stackcraft.api.content.domain.Chapter;
import dev.stackcraft.api.content.domain.Series;
import dev.stackcraft.api.content.port.ContentSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Adapter: content from files. Loads once at start-up (fail fast). With {@code reload=true} (dev profile) it
 * re-checks the folder at most once a second and swaps in a fresh snapshot when any file changed, so editing a
 * chapter shows up on the next page load without a restart. A broken edit keeps the last good snapshot and logs.
 */
public class FileSystemContentSource implements ContentSource {

    private static final Logger log = LoggerFactory.getLogger(FileSystemContentSource.class);
    private static final long CHECK_EVERY_MS = 1000;

    private final Path root;
    private final boolean reload;
    private final Clock clock;
    private final ContentLoader loader = new ContentLoader();

    private volatile ContentSnapshot snapshot;
    private volatile long fingerprint;
    private volatile long lastCheckMs;

    public FileSystemContentSource(Path root, boolean reload, Clock clock) {
        this.root = root;
        this.reload = reload;
        this.clock = clock;
        this.snapshot = loader.load(root);
        this.fingerprint = fingerprint();
        this.lastCheckMs = clock.millis();
        log.info("Loaded {} series and {} chapters from {}", snapshot.series().size(), snapshot.chapters().size(),
                root.toAbsolutePath().normalize());
    }

    @Override
    public List<Series> allSeries() {
        return current().series();
    }

    @Override
    public Optional<Series> series(String seriesId) {
        return current().series().stream().filter(s -> s.id().equals(seriesId)).findFirst();
    }

    @Override
    public Optional<Chapter> chapter(String chapterId) {
        return Optional.ofNullable(current().chapters().get(chapterId));
    }

    @Override
    public List<Chapter> allChapters() {
        Collection<Chapter> all = current().chapters().values();
        return all.stream().sorted((a, b) -> a.id().compareTo(b.id())).toList();
    }

    private ContentSnapshot current() {
        if (reload && clock.millis() - lastCheckMs >= CHECK_EVERY_MS) {
            maybeReload();
        }
        return snapshot;
    }

    private synchronized void maybeReload() {
        if (clock.millis() - lastCheckMs < CHECK_EVERY_MS) return;
        lastCheckMs = clock.millis();
        long now = fingerprint();
        if (now == fingerprint) return;
        try {
            snapshot = loader.load(root);
            fingerprint = now;
            log.info("Content changed: reloaded {} chapters", snapshot.chapters().size());
        } catch (RuntimeException e) {
            log.error("Content reload failed, keeping the previous version: {}", e.getMessage());
        }
    }

    /** Cheap change detector: combines every file's path, size and modified time. */
    private long fingerprint() {
        try (Stream<Path> files = Files.walk(root)) {
            return files.filter(Files::isRegularFile).mapToLong(p -> {
                try {
                    return p.toString().hashCode() * 31L + Files.getLastModifiedTime(p).toMillis() * 17L + Files.size(p);
                } catch (IOException e) {
                    return 0;
                }
            }).sum();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
