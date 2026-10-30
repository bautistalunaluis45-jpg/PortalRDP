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
import com.agentesocial.portalrdp.servicio.DepartamentoService;

/** Pantalla de administracion de departamentos (A4). Requiere ROLE_ADMIN (cadena de seguridad). */
@Controller
@RequestMapping("/web/admin/departamentos")
public class DepartamentosController {

    private final DepartamentoService departamentoService;

    public DepartamentosController(DepartamentoService departamentoService) {
        this.departamentoService = departamentoService;
    }

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("departamentos", departamentoService.listar());
        return "web/admin/departamentos";
    }

    @PostMapping
    public String crear(@RequestParam String nombre,
                        @RequestParam String codigo,
                        @RequestParam(required = false) String descripcion,
                        @AuthenticationPrincipal PrincipalPortal actor,
                        RedirectAttributes flash) {
        try {
            departamentoService.crear(nombre, codigo, descripcion, actor.getUsuarioId(), actor.getCorreo());
            flash.addFlashAttribute("exito", "Departamento creado.");
        } catch (ReglaNegocioException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/web/admin/departamentos";
    }

    @PostMapping("/{id}")
    public String editar(@PathVariable Long id,
                         @RequestParam String nombre,
                         @RequestParam String codigo,
                         @RequestParam(required = false) String descripcion,
                         @AuthenticationPrincipal PrincipalPortal actor,
                         RedirectAttributes flash) {
        try {
            departamentoService.editar(id, nombre, codigo, descripcion, actor.getUsuarioId(), actor.getCorreo());
            flash.addFlashAttribute("exito", "Departamento actualizado.");
        } catch (ReglaNegocioException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/web/admin/departamentos";
    }

    @PostMapping("/{id}/eliminar")
    public String eliminar(@PathVariable Long id,
                           @AuthenticationPrincipal PrincipalPortal actor,
                           RedirectAttributes flash) {
        try {
            departamentoService.eliminar(id, actor.getUsuarioId(), actor.getCorreo());
            flash.addFlashAttribute("exito", "Departamento eliminado.");
        } catch (ReglaNegocioException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/web/admin/departamentos";
    }
}
