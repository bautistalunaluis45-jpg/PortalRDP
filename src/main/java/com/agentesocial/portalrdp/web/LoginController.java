package com.agentesocial.portalrdp.web;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Pantalla de inicio de sesión del portal (perfil de Google, no "dev"): una sola opción,
 * "Iniciar sesión con Google". El botón apunta al endpoint estándar de Spring Security
 * /oauth2/authorization/google. En dev se usa /dev/login (login simulado).
 */
@Controller
@Profile("!dev")
public class LoginController {

    @GetMapping("/login")
    public String login() {
        return "web/login";
    }
}
