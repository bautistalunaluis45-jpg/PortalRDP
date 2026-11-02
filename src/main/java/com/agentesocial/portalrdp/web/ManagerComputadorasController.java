package com.agentesocial.portalrdp.web;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.agentesocial.portalrdp.dominio.TipoLanzamiento;
import com.agentesocial.portalrdp.excepcion.ReglaNegocioException;
import com.agentesocial.portalrdp.seguridad.PrincipalPortal;
import com.agentesocial.portalrdp.servicio.ComputadoraAdminVista;
import com.agentesocial.portalrdp.servicio.ComputadoraService;
import com.agentesocial.portalrdp.servicio.LanzamientoService;
import com.agentesocial.portalrdp.servicio.OrdenAgenteService;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Vista operativa del manager: todas las computadoras activas de todos los
 * departamentos, agrupadas por departamento, con operación (conectar / shadow /
 * desconectar). El manager no administra el CRUD; solo monitorea y opera.
 * Requiere ROLE_MANAGER (o ADMIN). Ver docs/decisiones.md sección 3.
 */
@Controller
@RequestMapping("/web/manager/computadoras")
public class ManagerComputadorasController {

    private final ComputadoraService computadoraService;
    private final LanzamientoService lanzamientoService;
    private final OrdenAgenteService ordenAgenteService;

    public ManagerComputadorasController(ComputadoraService computadoraService,
                                         LanzamientoService lanzamientoService,
                                         OrdenAgenteService ordenAgenteService) {
        this.computadoraService = computadoraService;
        this.lanzamientoService = lanzamientoService;
        this.ordenAgenteService = ordenAgenteService;
    }

    @GetMapping
    public String listar(Model model) {
        List<ComputadoraAdminVista> activas = computadoraService.listar().stream()
                .filter(ComputadoraAdminVista::activa)
                .toList();

        // Agrupadas por departamento (orden alfabético); dentro de cada grupo se
        // conserva el orden por nombre que ya trae el servicio.
        Map<String, List<ComputadoraAdminVista>> grupos = new TreeMap<>();
        for (ComputadoraAdminVista c : activas) {
            grupos.computeIfAbsent(c.departamentoNombre(), k -> new ArrayList<>()).add(c);
        }

        model.addAttribute("grupos", grupos);
        model.addAttribute("total", activas.size());
        model.addAttribute("libres", activas.stream().filter(ComputadoraAdminVista::esLibre).count());
        model.addAttribute("ocupadas", activas.stream().filter(ComputadoraAdminVista::esOcupada).count());
        model.addAttribute("sinConexion", activas.stream().filter(ComputadoraAdminVista::esSinConexion).count());
        return "web/manager/computadoras";
    }

    @PostMapping("/{id}/conectar")
    public String conectar(@PathVariable Long id, @AuthenticationPrincipal PrincipalPortal actor,
                           HttpServletRequest http, Model model) {
        model.addAttribute("emision", lanzamientoService.emitir(actor.getUsuarioId(), actor.getCorreo(),
                actor.getRol(), id, TipoLanzamiento.CONECTAR, http.getRemoteAddr()));
        return "web/lanzar";
    }

    @PostMapping("/{id}/shadow")
    public String shadow(@PathVariable Long id, @AuthenticationPrincipal PrincipalPortal actor,
                         HttpServletRequest http, Model model) {
        model.addAttribute("emision", lanzamientoService.emitir(actor.getUsuarioId(), actor.getCorreo(),
                actor.getRol(), id, TipoLanzamiento.SHADOW, http.getRemoteAddr()));
        return "web/lanzar";
    }

    @PostMapping("/{id}/desconectar")
    public String desconectar(@PathVariable Long id, @AuthenticationPrincipal PrincipalPortal actor, Model model) {
        try {
            model.addAttribute("orden",
                    ordenAgenteService.ordenarDesconectar(actor.getUsuarioId(), actor.getCorreo(), id));
        } catch (ReglaNegocioException e) {
            model.addAttribute("error", e.getMessage());
        }
        return "web/orden-enviada";
    }
}
