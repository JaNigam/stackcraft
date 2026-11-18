package dev.stackcraft.api.progress.port;

/** What a storage adapter implements: both sides. Use cases ask for the narrower interface they need. */
public interface ProgressStore extends ProgressReader, ProgressWriter {
}
