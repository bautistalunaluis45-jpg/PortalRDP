package com.agentesocial.portalrdp.web;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import com.agentesocial.portalrdp.seguridad.PrincipalPortal;

/** Expone el usuario autenticado a todas las vistas de /web (lo usa el layout). */
@ControllerAdvice(basePackages = "com.agentesocial.portalrdp.web")
public class AtributosWebAdvice {

    @ModelAttribute("usuario")
    public PrincipalPortal usuario(@AuthenticationPrincipal PrincipalPortal usuario) {
        return usuario;
    }
}
