package dev.stackcraft.runner;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/** Entry point. The runner is internal only: the api reaches it over a private network, never the browser. */
@SpringBootApplication
@ConfigurationPropertiesScan
public class RunnerApplication {
    public static void main(String[] args) {
        SpringApplication.run(RunnerApplication.class, args);
    }
}
