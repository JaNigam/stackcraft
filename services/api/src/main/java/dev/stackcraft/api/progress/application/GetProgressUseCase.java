package dev.stackcraft.api.progress.application;

import dev.stackcraft.api.progress.domain.LearnerProgress;
import dev.stackcraft.api.progress.port.ProgressReader;

import java.time.Clock;
import java.time.LocalDate;

public class GetProgressUseCase {

    private final ProgressReader reader;
    private final Clock clock;

    public GetProgressUseCase(ProgressReader reader, Clock clock) {
        this.reader = reader;
        this.clock = clock;
    }

    public LearnerProgress get(String learnerId) {
        return LearnerProgress.from(learnerId, reader.events(learnerId), LocalDate.now(clock), clock.getZone());
    }
}
