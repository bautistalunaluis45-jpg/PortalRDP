package com.agentesocial.portalrdp.web;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import com.agentesocial.portalrdp.dominio.Rol;
import com.agentesocial.portalrdp.seguridad.PrincipalPortal;

/** Entrada del portal. Aterriza segun el rol en la primera pantalla de su menu. */
@Controller
public class InicioController {

    @GetMapping({"/", "/web/"})
    public String raiz(@AuthenticationPrincipal PrincipalPortal usuario) {
        if (usuario != null && usuario.getRol() == Rol.ADMIN) {
            return "redirect:/web/admin/departamentos";
        }
        if (usuario != null && usuario.getRol() == Rol.MANAGER) {
            return "redirect:/web/manager/computadoras";
        }
        // Empleado: sus computadoras asignadas y accesos temporales.
        return "redirect:/web/mis-computadoras";
    }
}
