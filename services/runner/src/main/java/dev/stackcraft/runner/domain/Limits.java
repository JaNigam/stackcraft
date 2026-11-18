package dev.stackcraft.runner.domain;

import java.time.Duration;

/**
 * Hard limits for one run. Every run gets the same limits; the caller cannot raise them (least privilege).
 *
 * @param wallClock      total time the child JVM may live
 * @param maxHeapMb      -Xmx for the child JVM
 * @param outputCapBytes max bytes kept per stream; going past it kills the process
 */
public record Limits(Duration wallClock, int maxHeapMb, int outputCapBytes) {
    public Limits {
        if (wallClock.isNegative() || wallClock.isZero()) throw new IllegalArgumentException("wallClock must be > 0");
        if (maxHeapMb < 16) throw new IllegalArgumentException("maxHeapMb must be >= 16");
        if (outputCapBytes < 1) throw new IllegalArgumentException("outputCapBytes must be > 0");
    }
}
