package dev.stackcraft.api.identity.adapter;

import dev.stackcraft.api.identity.port.LoginThrottle;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Sliding window: at most {@code maxFailures} failures per key in {@code window}. Fine for one instance; swap for a
 * Redis adapter (same port) once the api runs more than one replica.
 */
public class InMemoryLoginThrottle implements LoginThrottle {

    private final int maxFailures;
    private final Duration window;
    private final Clock clock;
    private final ConcurrentHashMap<String, Deque<Instant>> failures = new ConcurrentHashMap<>();

    public InMemoryLoginThrottle(int maxFailures, Duration window, Clock clock) {
        this.maxFailures = maxFailures;
        this.window = window;
        this.clock = clock;
    }

    @Override
    public boolean allowed(String key) {
        Deque<Instant> d = failures.get(key);
        if (d == null) return true;
        synchronized (d) {
            prune(d);
            return d.size() < maxFailures;
        }
    }

    @Override
    public void recordFailure(String key) {
        Deque<Instant> d = failures.computeIfAbsent(key, k -> new ArrayDeque<>());
        synchronized (d) {
            prune(d);
            d.addLast(clock.instant());
        }
        if (failures.size() > 50_000) failures.entrySet().removeIf(e -> e.getValue().isEmpty());
    }

    @Override
    public void reset(String key) {
        failures.remove(key);
    }

    private void prune(Deque<Instant> d) {
        Instant cutoff = clock.instant().minus(window);
        while (!d.isEmpty() && d.peekFirst().isBefore(cutoff)) d.removeFirst();
    }
}
