package com.agentesocial.portalrdp.seguridad;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Seguridad de la API del agente (/api/agente/**): sin Google, sin CSRF, sin sesión.
 * La autenticación es por token por PC (hash), que verifica AgenteService en cada latido.
 * Orden alto para que atienda estas rutas antes que la cadena web (que no tiene matcher).
 */
@Configuration
public class SeguridadApiAgenteConfig {

    @Bean
    @Order(1)
    SecurityFilterChain cadenaApiAgente(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/api/agente/**")
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(reglas -> reglas.anyRequest().permitAll());
        return http.build();
    }
}
