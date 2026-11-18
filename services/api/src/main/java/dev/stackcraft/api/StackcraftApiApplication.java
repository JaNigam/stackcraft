package dev.stackcraft.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class StackcraftApiApplication {
    public static void main(String[] args) {
        SpringApplication.run(StackcraftApiApplication.class, args);
    }
}
