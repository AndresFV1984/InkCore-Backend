package com.inkcore.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

@Configuration
public class ClockConfig {

    /** Zona de negocio (Colombia). LocalDateTime naive = hora local, no UTC. */
    public static final ZoneId BUSINESS_ZONE = ZoneId.of("America/Bogota");

    @Bean
    Clock clock() {
        return Clock.system(BUSINESS_ZONE);
    }
}
