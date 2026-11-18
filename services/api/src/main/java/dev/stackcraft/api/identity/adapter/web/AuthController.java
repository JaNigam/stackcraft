package dev.stackcraft.api.identity.adapter.web;

import dev.stackcraft.api.identity.application.LoginUseCase;
import dev.stackcraft.api.identity.application.LogoutUseCase;
import dev.stackcraft.api.identity.application.RefreshSessionUseCase;
import dev.stackcraft.api.identity.application.RegisterUseCase;
import dev.stackcraft.api.identity.application.Session;
import dev.stackcraft.api.identity.domain.IdentityException;
import dev.stackcraft.api.shared.CurrentLearner;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final RegisterUseCase register;
    private final LoginUseCase login;
    private final RefreshSessionUseCase refresh;
    private final LogoutUseCase logout;
    private final SessionCookies cookies;
    private final CurrentLearner learner;

    public AuthController(RegisterUseCase register, LoginUseCase login, RefreshSessionUseCase refresh, LogoutUseCase logout,
                          SessionCookies cookies, CurrentLearner learner) {
        this.register = register;
        this.login = login;
        this.refresh = refresh;
        this.logout = logout;
        this.cookies = cookies;
        this.learner = learner;
    }

    @PostMapping("/register")
    public ResponseEntity<MeView> register(@Valid @RequestBody RegisterBody body, HttpServletResponse res) {
        Session s = register.register(body.email(), body.password(), body.displayName());
        cookies.write(res, s);
        return ResponseEntity.status(HttpStatus.CREATED).body(MeView.of(s.user()));
    }

    @PostMapping("/login")
    public MeView login(@Valid @RequestBody LoginBody body, HttpServletRequest req, HttpServletResponse res) {
        Session s = login.login(body.email(), body.password(), req.getRemoteAddr());
        cookies.write(res, s);
        return MeView.of(s.user());
    }

    @PostMapping("/refresh")
    public MeView refresh(@CookieValue(name = SessionCookies.REFRESH, required = false) String token, HttpServletResponse res) {
        try {
            Session s = refresh.refresh(token);
            cookies.write(res, s);
            return MeView.of(s.user());
        } catch (IdentityException.InvalidSession e) {
            cookies.clear(res);
            throw e;
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@CookieValue(name = SessionCookies.REFRESH, required = false) String token, HttpServletResponse res) {
        logout.logout(token);
        cookies.clear(res);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/logout-all")
    public ResponseEntity<Void> logoutAll(HttpServletResponse res) {
        logout.logoutEverywhere(UUID.fromString(learner.id()));
        cookies.clear(res);
        return ResponseEntity.noContent().build();
    }

    public record RegisterBody(@NotNull @Size(max = 254) String email, @NotNull @Size(max = 128) String password,
                               @NotNull @Size(max = 64) String displayName) {}

    public record LoginBody(@NotNull @Size(max = 254) String email, @NotNull @Size(max = 128) String password) {}
}
