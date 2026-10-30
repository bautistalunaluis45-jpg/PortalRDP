package com.agentesocial.portalrdp.web;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.agentesocial.portalrdp.excepcion.ReglaNegocioException;
import com.agentesocial.portalrdp.seguridad.PrincipalPortal;
import com.agentesocial.portalrdp.servicio.AccesoTemporalService;
import com.agentesocial.portalrdp.servicio.ComputadoraService;
import com.agentesocial.portalrdp.servicio.UsuarioService;

/** Accesos temporales (A8, A9). Requiere ROLE_ADMIN. */
@Controller
@RequestMapping("/web/admin/accesos-temporales")
public class AccesosTemporalesController {

    private final AccesoTemporalService accesoTemporalService;
    private final UsuarioService usuarioService;
    private final ComputadoraService computadoraService;

    public AccesosTemporalesController(AccesoTemporalService accesoTemporalService,
                                       UsuarioService usuarioService,
                                       ComputadoraService computadoraService) {
        this.accesoTemporalService = accesoTemporalService;
        this.usuarioService = usuarioService;
        this.computadoraService = computadoraService;
    }

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("accesos", accesoTemporalService.listarVigentes());
        model.addAttribute("usuarios", usuarioService.listar());
        model.addAttribute("computadoras", computadoraService.listar());
        return "web/admin/accesos-temporales";
    }

    @PostMapping
    public String otorgar(@RequestParam(required = false) Long usuarioId,
                          @RequestParam(required = false) Long computadoraId,
                          @RequestParam(required = false) String duracion,
                          @RequestParam(required = false) String motivo,
                          @AuthenticationPrincipal PrincipalPortal actor,
                          RedirectAttributes flash) {
        try {
            Integer horas = parseDuracion(duracion);
            accesoTemporalService.otorgar(usuarioId, computadoraId, horas, motivo,
                    actor.getUsuarioId(), actor.getCorreo());
            flash.addFlashAttribute("exito", "Acceso temporal otorgado.");
        } catch (NumberFormatException e) {
            flash.addFlashAttribute("error", "Duración no válida.");
        } catch (ReglaNegocioException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/web/admin/accesos-temporales";
    }

    /** "indefinido" (o vacío) => null (sin vencimiento); de lo contrario, horas. */
    private static Integer parseDuracion(String duracion) {
        if (duracion == null || duracion.isBlank() || "indefinido".equalsIgnoreCase(duracion.trim())) {
            return null;
        }
        return Integer.valueOf(duracion.trim());
    }

    @PostMapping("/{id}/revocar")
    public String revocar(@PathVariable Long id,
                          @AuthenticationPrincipal PrincipalPortal actor,
                          RedirectAttributes flash) {
        try {
            accesoTemporalService.revocar(id, actor.getUsuarioId(), actor.getCorreo());
            flash.addFlashAttribute("exito", "Acceso temporal revocado.");
        } catch (ReglaNegocioException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/web/admin/accesos-temporales";
    }
}
