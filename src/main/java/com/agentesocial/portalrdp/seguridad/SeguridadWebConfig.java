package com.agentesocial.portalrdp.seguridad;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Seguridad del portal web (/web/**) en todos los entornos menos "dev".
 * Login con Google (OIDC). Sesion de servidor y CSRF activos (por defecto).
 */
@Configuration
@Profile("!dev")
public class SeguridadWebConfig {

    @Bean
    @Order(2)
    SecurityFilterChain cadenaWeb(HttpSecurity http, OidcUsuarioService oidcUsuarioService) throws Exception {
        http
                .authorizeHttpRequests(reglas -> reglas
                        .requestMatchers("/login", "/css/**", "/js/**", "/favicon.ico", "/error").permitAll()
                        .requestMatchers("/descargas/**").permitAll()
                        .requestMatchers("/web/admin/**").hasRole("ADMIN")
                        .requestMatchers("/web/manager/**", "/web/resumen").hasAnyRole("MANAGER", "ADMIN")
                        .anyRequest().authenticated())
                .oauth2Login(login -> login
                        .loginPage("/login")
                        .userInfoEndpoint(info -> info.oidcUserService(oidcUsuarioService))
                        .defaultSuccessUrl("/web/", true))
                .logout(logout -> logout.logoutSuccessUrl("/login?salida"));
        return http.build();
    }
}
