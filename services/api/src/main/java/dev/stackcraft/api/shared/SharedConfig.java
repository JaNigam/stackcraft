package dev.stackcraft.api.shared;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.time.Clock;
import java.time.ZoneId;

@Configuration(proxyBeanMethods = false)
public class SharedConfig {

    /** Injected everywhere time matters, so tests can freeze it (streaks, due dates). */
    @Bean
    Clock clock(StackcraftProperties props) {
        return Clock.system(ZoneId.of(props.zone()));
    }

    /** CORS locked to the web origin(s); nothing else may call the api from a browser. */
    @Bean
    WebMvcConfigurer cors(StackcraftProperties props) {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/api/**")
                        .allowedOrigins(props.webOrigins().toArray(String[]::new))
                        .allowedMethods("GET", "POST", "PATCH")
                        .allowedHeaders("Content-Type", "X-Request-Id", "X-XSRF-TOKEN")
                        .allowCredentials(true)
                        .exposedHeaders("X-Request-Id")
                        .maxAge(3600);
            }
        };
    }
}
