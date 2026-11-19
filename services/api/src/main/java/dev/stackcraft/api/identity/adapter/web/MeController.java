package dev.stackcraft.api.identity.adapter.web;

import dev.stackcraft.api.identity.application.ProfileUseCase;
import dev.stackcraft.api.shared.CurrentLearner;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/me")
public class MeController {

    private final ProfileUseCase profile;
    private final CurrentLearner learner;

    public MeController(ProfileUseCase profile, CurrentLearner learner) {
        this.profile = profile;
        this.learner = learner;
    }

    @GetMapping
    public MeView me() {
        return MeView.of(profile.me(UUID.fromString(learner.id())));
    }

    @PatchMapping
    public MeView update(@Valid @RequestBody ProfileUpdate body) {
        return MeView.of(profile.update(UUID.fromString(learner.id()), body.displayName(), body.leaderboardVisible()));
    }

    public record ProfileUpdate(@Size(max = 64) String displayName, Boolean leaderboardVisible) {}
}
