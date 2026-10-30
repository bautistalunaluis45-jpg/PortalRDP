package com.agentesocial.portalrdp.infraestructura;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Beans de infraestructura. El reloj es inyectable para poder fijarlo en las pruebas. */
@Configuration
public class ConfiguracionInfraestructura {

    @Bean
    public Clock reloj() {
        return Clock.systemDefaultZone();
    }
}
