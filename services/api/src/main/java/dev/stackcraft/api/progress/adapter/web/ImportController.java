package dev.stackcraft.api.progress.adapter.web;

import dev.stackcraft.api.progress.application.GetProgressUseCase;
import dev.stackcraft.api.progress.application.ImportGuestProgressUseCase;
import dev.stackcraft.api.progress.application.ImportGuestProgressUseCase.GuestCard;
import dev.stackcraft.api.progress.application.ImportGuestProgressUseCase.GuestEvent;
import dev.stackcraft.api.progress.application.ImportGuestProgressUseCase.GuestProgress;
import dev.stackcraft.api.progress.application.ImportGuestProgressUseCase.GuestTrap;
import dev.stackcraft.api.shared.CurrentLearner;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class ImportController {

    private final ImportGuestProgressUseCase importer;
    private final GetProgressUseCase progress;
    private final CurrentLearner learner;

    public ImportController(ImportGuestProgressUseCase importer, GetProgressUseCase progress, CurrentLearner learner) {
        this.importer = importer;
        this.progress = progress;
        this.learner = learner;
    }

    @PostMapping("/api/v1/progress/imports")
    public ImportResponse importGuest(@Valid @RequestBody ImportBody body) {
        var r = importer.importGuest(learner.id(), new GuestProgress(body.events(), body.traps(), body.cards()));
        return new ImportResponse(r.eventsImported(), r.eventsSkipped(), r.xpAwarded(), ProgressView.of(progress.get(learner.id())));
    }

    public record ImportBody(@NotNull @Size(max = ImportGuestProgressUseCase.MAX_EVENTS) List<GuestEvent> events,
                             @Size(max = 500) List<GuestTrap> traps,
                             @Size(max = 1000) List<GuestCard> cards) {}

    public record ImportResponse(int eventsImported, int eventsSkipped, int xpAwarded, ProgressView progress) {}
}
