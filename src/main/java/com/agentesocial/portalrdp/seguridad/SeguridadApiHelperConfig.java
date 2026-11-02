package com.agentesocial.portalrdp.seguridad;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Seguridad de la API del helper (/api/helper/**): sin Google, sin CSRF, sin sesión.
 * La autenticación es el propio token de lanzamiento (de un solo uso y corta vida),
 * que LanzamientoService valida al canjear. Ver docs/decisiones.md 5, 6 (regla 6).
 */
@Configuration
public class SeguridadApiHelperConfig {

    @Bean
    @Order(0)
    SecurityFilterChain cadenaApiHelper(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/api/helper/**")
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(reglas -> reglas.anyRequest().permitAll());
        return http.build();
    }
}
