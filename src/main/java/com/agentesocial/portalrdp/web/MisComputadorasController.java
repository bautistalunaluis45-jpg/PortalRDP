package com.agentesocial.portalrdp.web;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.agentesocial.portalrdp.dominio.TipoLanzamiento;
import com.agentesocial.portalrdp.seguridad.PrincipalPortal;
import com.agentesocial.portalrdp.servicio.LanzamientoService;
import com.agentesocial.portalrdp.servicio.VisibilidadService;

import jakarta.servlet.http.HttpServletRequest;

/** Pantalla "Mis computadoras" del empleado (casos E1, E2) y su botón Conectar (fase 5). */
@Controller
@RequestMapping("/web")
public class MisComputadorasController {

    private final VisibilidadService visibilidadService;
    private final LanzamientoService lanzamientoService;

    public MisComputadorasController(VisibilidadService visibilidadService,
                                     LanzamientoService lanzamientoService) {
        this.visibilidadService = visibilidadService;
        this.lanzamientoService = lanzamientoService;
    }

    @GetMapping("/mis-computadoras")
    public String misComputadoras(@AuthenticationPrincipal PrincipalPortal usuario, Model model) {
        model.addAttribute("usuario", usuario);
        model.addAttribute("vista", visibilidadService.misComputadoras(usuario.getUsuarioId()));
        return "web/mis-computadoras";
    }

    /**
     * Conectar: valida (visibilidad + PC libre), emite el token de un solo uso y manda a
     * la pantalla que abre el protocolo del helper (docs/decisiones.md 5). Si se rechaza,
     * la misma pantalla muestra el motivo (ya quedó auditado).
     */
    @PostMapping("/mis-computadoras/{id}/conectar")
    public String conectar(@PathVariable Long id,
                           @AuthenticationPrincipal PrincipalPortal usuario,
                           HttpServletRequest http,
                           Model model) {
        model.addAttribute("usuario", usuario);
        model.addAttribute("emision", lanzamientoService.emitir(usuario.getUsuarioId(), usuario.getCorreo(),
                usuario.getRol(), id, TipoLanzamiento.CONECTAR, http.getRemoteAddr()));
        return "web/lanzar";
    }
}
