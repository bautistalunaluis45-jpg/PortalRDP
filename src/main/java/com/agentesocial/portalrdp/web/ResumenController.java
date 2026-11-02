package com.agentesocial.portalrdp.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.agentesocial.portalrdp.servicio.ResumenService;

/** Panel "Resumen en vivo" para manager y admin (gating en la cadena de seguridad). */
@Controller
public class ResumenController {

    private final ResumenService resumenService;

    public ResumenController(ResumenService resumenService) {
        this.resumenService = resumenService;
    }

    @GetMapping("/web/resumen")
    public String resumen(Model model) {
        model.addAttribute("r", resumenService.enVivo());
        return "web/resumen";
    }
}
