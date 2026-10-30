package com.agentesocial.portalrdp.seguridad.dev;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;

/**
 * Seguridad SOLO para desarrollo (perfil "dev"): sustituye el login con Google
 * por un login simulado (ver DevLoginController). No se usa Google en dev.
 */
@Configuration
@Profile("dev")
public class SeguridadDevConfig {

    @Bean
    SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    @Order(2)
    SecurityFilterChain cadenaDev(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(reglas -> reglas
                        .requestMatchers("/dev/login", "/css/**", "/js/**", "/favicon.ico", "/error").permitAll()
                        .requestMatchers("/web/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(new LoginUrlAuthenticationEntryPoint("/dev/login")))
                .logout(logout -> logout.logoutSuccessUrl("/dev/login"));
        return http.build();
    }
}
