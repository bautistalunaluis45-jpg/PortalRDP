package com.agentesocial.portalrdp.web;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.agentesocial.portalrdp.dominio.AtajosRdp;
import com.agentesocial.portalrdp.dominio.CalidadRdp;
import com.agentesocial.portalrdp.dominio.SonidoRdp;
import com.agentesocial.portalrdp.excepcion.ReglaNegocioException;
import com.agentesocial.portalrdp.seguridad.PrincipalPortal;
import com.agentesocial.portalrdp.servicio.PreferenciasService;

/** Preferencias RDP del usuario autenticado (E3). Disponible para los tres roles. */
@Controller
@RequestMapping("/web/preferencias")
public class PreferenciasController {

    private final PreferenciasService preferenciasService;

    public PreferenciasController(PreferenciasService preferenciasService) {
        this.preferenciasService = preferenciasService;
    }

    @GetMapping
    public String ver(@AuthenticationPrincipal PrincipalPortal usuario, Model model) {
        model.addAttribute("prefs", preferenciasService.obtener(usuario.getUsuarioId()));
        return "web/preferencias";
    }

    @PostMapping
    public String guardar(@RequestParam(defaultValue = "false") boolean ajusteAutomatico,
                          @RequestParam(defaultValue = "false") boolean pantallaCompleta,
                          @RequestParam(defaultValue = "false") boolean multimonitor,
                          @RequestParam(defaultValue = "100") int escala,
                          @RequestParam(required = false) String sonido,
                          @RequestParam(defaultValue = "false") boolean microfono,
                          @RequestParam(required = false) String atajos,
                          @RequestParam(required = false) String calidad,
                          @AuthenticationPrincipal PrincipalPortal usuario,
                          RedirectAttributes flash) {
        try {
            preferenciasService.guardar(usuario.getUsuarioId(), ajusteAutomatico, pantallaCompleta, multimonitor,
                    escala, parse(SonidoRdp.class, sonido), microfono,
                    parse(AtajosRdp.class, atajos), parse(CalidadRdp.class, calidad));
            flash.addFlashAttribute("exito", "Preferencias guardadas. Se aplicarán en tu próxima conexión.");
        } catch (ReglaNegocioException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/web/preferencias";
    }

    private static <E extends Enum<E>> E parse(Class<E> tipo, String valor) {
        if (valor == null || valor.isBlank()) {
            throw new ReglaNegocioException("Falta una opción obligatoria.");
        }
        try {
            return Enum.valueOf(tipo, valor.trim());
        } catch (IllegalArgumentException e) {
            throw new ReglaNegocioException("Opción no válida: " + valor);
        }
    }
}
