package com.agentesocial.portalrdp.web;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.agentesocial.portalrdp.seguridad.PrincipalPortal;
import com.agentesocial.portalrdp.servicio.VisibilidadService;

/** Pantalla "Mis computadoras" del empleado (casos E1, E2). */
@Controller
@RequestMapping("/web")
public class MisComputadorasController {

    private final VisibilidadService visibilidadService;

    public MisComputadorasController(VisibilidadService visibilidadService) {
        this.visibilidadService = visibilidadService;
    }

    @GetMapping("/mis-computadoras")
    public String misComputadoras(@AuthenticationPrincipal PrincipalPortal usuario, Model model) {
        model.addAttribute("usuario", usuario);
        model.addAttribute("vista", visibilidadService.misComputadoras(usuario.getUsuarioId()));
        return "web/mis-computadoras";
    }

    /**
     * Marcador del boton Conectar. El lanzamiento real (validar visibilidad y que la
     * PC este libre, emitir el token de un solo uso y abrir mstsc por el helper) es la
     * fase 5 (docs/decisiones.md seccion 5). Aqui no se lanza nada todavia.
     */
    @PostMapping("/mis-computadoras/{id}/conectar")
    public String conectar(@PathVariable Long id,
                           @AuthenticationPrincipal PrincipalPortal usuario,
                           Model model) {
        model.addAttribute("usuario", usuario);
        model.addAttribute("computadoraId", id);
        return "web/conectar-pendiente";
    }
}
