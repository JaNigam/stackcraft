package dev.stackcraft.api.identity.adapter.jpa;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration(proxyBeanMethods = false)
@EnableJpaRepositories(considerNestedRepositories = true)
public class JpaIdentityConfig {

    @Bean
    JpaIdentityStore identityStore(JpaIdentityStore.Users users, JpaIdentityStore.Tokens tokens) {
        return new JpaIdentityStore(users, tokens);
    }
}
