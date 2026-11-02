package com.agentesocial.portalrdp.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;

import com.agentesocial.portalrdp.infraestructura.PropiedadesHelper;

/** Pantalla de Ayuda (todos los roles): descarga e instalación del helper PulseDesk. */
@Controller
public class AyudaController {

    private final PropiedadesHelper propiedadesHelper;

    public AyudaController(PropiedadesHelper propiedadesHelper) {
        this.propiedadesHelper = propiedadesHelper;
    }

    @GetMapping("/web/ayuda")
    public String ayuda(Model model) {
        model.addAttribute("helperDisponible", StringUtils.hasText(propiedadesHelper.getArchivo()));
        return "web/ayuda";
    }
}
