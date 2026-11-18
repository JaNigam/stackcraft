package dev.stackcraft.api.progress.adapter.jpa;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration(proxyBeanMethods = false)
@EnableJpaRepositories(considerNestedRepositories = true)
public class JpaProgressConfig {

    @Bean
    JpaProgressStore progressStore(Repositories.XpEvents events, Repositories.Traps traps, Repositories.Flashcards cards) {
        return new JpaProgressStore(events, traps, cards);
    }
}
