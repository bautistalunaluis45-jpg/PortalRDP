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

import com.agentesocial.portalrdp.dominio.Rol;
import com.agentesocial.portalrdp.excepcion.ReglaNegocioException;
import com.agentesocial.portalrdp.seguridad.PrincipalPortal;
import com.agentesocial.portalrdp.servicio.AuditoriaService;
import com.agentesocial.portalrdp.servicio.DepartamentoService;
import com.agentesocial.portalrdp.servicio.UsuarioService;

/** Administracion de usuarios (A1, A2, A3). Requiere ROLE_ADMIN (cadena de seguridad). */
@Controller
@RequestMapping("/web/admin/usuarios")
public class UsuariosController {

    private final UsuarioService usuarioService;
    private final DepartamentoService departamentoService;
    private final AuditoriaService auditoriaService;

    public UsuariosController(UsuarioService usuarioService,
                              DepartamentoService departamentoService,
                              AuditoriaService auditoriaService) {
        this.usuarioService = usuarioService;
        this.departamentoService = departamentoService;
        this.auditoriaService = auditoriaService;
    }

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("usuarios", usuarioService.listar());
        model.addAttribute("resumen", usuarioService.resumen());
        return "web/admin/usuarios";
    }

    @GetMapping("/nuevo")
    public String formularioNuevo(Model model) {
        model.addAttribute("departamentos", departamentoService.listar());
        return "web/admin/usuario-form";
    }

    @PostMapping
    public String crear(@RequestParam String nombre,
                        @RequestParam String correo,
                        @RequestParam(required = false) Long departamentoId,
                        @RequestParam(required = false) String rol,
                        @AuthenticationPrincipal PrincipalPortal actor,
                        RedirectAttributes flash) {
        try {
            Long id = usuarioService.crear(nombre, correo, departamentoId, parseRol(rol),
                    actor.getUsuarioId(), actor.getCorreo());
            flash.addFlashAttribute("exito", "Usuario creado.");
            return "redirect:/web/admin/usuarios/" + id;
        } catch (ReglaNegocioException e) {
            flash.addFlashAttribute("error", e.getMessage());
            return "redirect:/web/admin/usuarios/nuevo";
        }
    }

    @GetMapping("/{id}")
    public String detalle(@PathVariable Long id, Model model, RedirectAttributes flash) {
        try {
            var u = usuarioService.obtener(id);
            model.addAttribute("u", u);
            model.addAttribute("departamentos", departamentoService.listar());
            model.addAttribute("actividad", auditoriaService.ultimosDeUsuario(id));
            model.addAttribute("ultimoAcceso", auditoriaService.ultimoAcceso(id));
            model.addAttribute("pcsDepartamento", usuarioService.computadorasEnDepartamento(u.departamentoId()));
            return "web/admin/usuario-detalle";
        } catch (ReglaNegocioException e) {
            flash.addFlashAttribute("error", e.getMessage());
            return "redirect:/web/admin/usuarios";
        }
    }

    @GetMapping("/{id}/auditoria")
    public String auditoria(@PathVariable Long id, Model model, RedirectAttributes flash) {
        try {
            model.addAttribute("u", usuarioService.obtener(id));
            model.addAttribute("historial", auditoriaService.historialDeUsuario(id));
            return "web/admin/usuario-auditoria";
        } catch (ReglaNegocioException e) {
            flash.addFlashAttribute("error", e.getMessage());
            return "redirect:/web/admin/usuarios";
        }
    }

    @PostMapping("/{id}")
    public String actualizar(@PathVariable Long id,
                             @RequestParam String nombre,
                             @RequestParam(required = false) Long departamentoId,
                             @RequestParam(required = false) String rol,
                             @AuthenticationPrincipal PrincipalPortal actor,
                             RedirectAttributes flash) {
        try {
            usuarioService.actualizar(id, nombre, departamentoId, parseRol(rol),
                    actor.getUsuarioId(), actor.getCorreo());
            flash.addFlashAttribute("exito", "Usuario actualizado.");
        } catch (ReglaNegocioException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/web/admin/usuarios/" + id;
    }

    @PostMapping("/{id}/estado")
    public String cambiarEstado(@PathVariable Long id,
                                @RequestParam boolean activo,
                                @AuthenticationPrincipal PrincipalPortal actor,
                                RedirectAttributes flash) {
        try {
            usuarioService.cambiarEstado(id, activo, actor.getUsuarioId(), actor.getCorreo());
            flash.addFlashAttribute("exito", activo ? "Usuario reactivado." : "Usuario dado de baja.");
        } catch (ReglaNegocioException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/web/admin/usuarios/" + id;
    }

    @PostMapping("/{id}/eliminar")
    public String eliminar(@PathVariable Long id,
                           @AuthenticationPrincipal PrincipalPortal actor,
                           RedirectAttributes flash) {
        try {
            usuarioService.eliminar(id, actor.getUsuarioId(), actor.getCorreo());
            flash.addFlashAttribute("exito", "Usuario eliminado definitivamente. Su historial de auditoría se conserva.");
        } catch (ReglaNegocioException e) {
            flash.addFlashAttribute("error", e.getMessage());
            return "redirect:/web/admin/usuarios/" + id;
        }
        return "redirect:/web/admin/usuarios";
    }

    private static Rol parseRol(String rol) {
        if (rol == null || rol.isBlank()) {
            throw new ReglaNegocioException("El rol es obligatorio.");
        }
        try {
            return Rol.valueOf(rol.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ReglaNegocioException("Rol no válido.");
        }
    }
}
