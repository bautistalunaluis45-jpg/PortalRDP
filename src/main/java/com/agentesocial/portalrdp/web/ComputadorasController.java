package com.agentesocial.portalrdp.web;

import java.nio.file.Files;
import java.nio.file.Path;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.agentesocial.portalrdp.dominio.TipoLanzamiento;
import com.agentesocial.portalrdp.excepcion.ReglaNegocioException;
import com.agentesocial.portalrdp.infraestructura.PropiedadesAgente;
import com.agentesocial.portalrdp.seguridad.PrincipalPortal;
import com.agentesocial.portalrdp.servicio.ComputadoraService;
import com.agentesocial.portalrdp.servicio.DepartamentoService;
import com.agentesocial.portalrdp.servicio.LanzamientoService;
import com.agentesocial.portalrdp.servicio.OrdenAgenteService;

import jakarta.servlet.http.HttpServletRequest;

/** Administración de computadoras (A5, A6, A7). Requiere ROLE_ADMIN. */
@Controller
@RequestMapping("/web/admin/computadoras")
public class ComputadorasController {

    private final ComputadoraService computadoraService;
    private final DepartamentoService departamentoService;
    private final PropiedadesAgente propiedadesAgente;
    private final LanzamientoService lanzamientoService;
    private final OrdenAgenteService ordenAgenteService;

    public ComputadorasController(ComputadoraService computadoraService, DepartamentoService departamentoService,
                                  PropiedadesAgente propiedadesAgente, LanzamientoService lanzamientoService,
                                  OrdenAgenteService ordenAgenteService) {
        this.computadoraService = computadoraService;
        this.departamentoService = departamentoService;
        this.propiedadesAgente = propiedadesAgente;
        this.lanzamientoService = lanzamientoService;
        this.ordenAgenteService = ordenAgenteService;
    }

    /** Descarga del ejecutable del agente para instalar en las PCs destino. */
    @GetMapping("/agente/descargar")
    public ResponseEntity<Resource> descargarAgente() {
        String ruta = propiedadesAgente.getArchivo();
        if (!StringUtils.hasText(ruta)) {
            return ResponseEntity.notFound().build();
        }
        Path archivo = Path.of(ruta);
        if (!Files.isReadable(archivo) || Files.isDirectory(archivo)) {
            return ResponseEntity.notFound().build();
        }
        long tamano;
        try {
            tamano = Files.size(archivo);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + archivo.getFileName() + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .contentLength(tamano)
                .body(new FileSystemResource(archivo));
    }

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("computadoras", computadoraService.listar());
        model.addAttribute("resumen", computadoraService.resumen());
        return "web/admin/computadoras";
    }

    @GetMapping("/nueva")
    public String formularioNueva(Model model) {
        model.addAttribute("departamentos", departamentoService.listar());
        return "web/admin/computadora-form";
    }

    @PostMapping
    public String crear(@RequestParam String nombre,
                        @RequestParam String direccionTailscale,
                        @RequestParam String cuentaUsuario,
                        @RequestParam(required = false) Long departamentoId,
                        @RequestParam(required = false) String password,
                        @AuthenticationPrincipal PrincipalPortal actor,
                        RedirectAttributes flash) {
        try {
            Long id = computadoraService.crear(nombre, direccionTailscale, cuentaUsuario, departamentoId,
                    password, actor.getUsuarioId(), actor.getCorreo());
            flash.addFlashAttribute("exito", "Computadora creada.");
            return "redirect:/web/admin/computadoras/" + id;
        } catch (ReglaNegocioException e) {
            flash.addFlashAttribute("error", e.getMessage());
            return "redirect:/web/admin/computadoras/nueva";
        }
    }

    @GetMapping("/{id}")
    public String detalle(@PathVariable Long id, Model model, RedirectAttributes flash) {
        try {
            model.addAttribute("c", computadoraService.obtener(id));
            model.addAttribute("departamentos", departamentoService.listar());
            return "web/admin/computadora-detalle";
        } catch (ReglaNegocioException e) {
            flash.addFlashAttribute("error", e.getMessage());
            return "redirect:/web/admin/computadoras";
        }
    }

    @PostMapping("/{id}")
    public String actualizar(@PathVariable Long id,
                             @RequestParam String nombre,
                             @RequestParam String direccionTailscale,
                             @RequestParam String cuentaUsuario,
                             @RequestParam(required = false) Long departamentoId,
                             @RequestParam(required = false) String password,
                             @AuthenticationPrincipal PrincipalPortal actor,
                             RedirectAttributes flash) {
        try {
            computadoraService.actualizar(id, nombre, direccionTailscale, cuentaUsuario, departamentoId,
                    password, actor.getUsuarioId(), actor.getCorreo());
            flash.addFlashAttribute("exito", "Computadora actualizada.");
        } catch (ReglaNegocioException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/web/admin/computadoras/" + id;
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

    @PostMapping("/{id}/token-agente")
    public String generarTokenAgente(@PathVariable Long id,
                                     @AuthenticationPrincipal PrincipalPortal actor,
                                     RedirectAttributes flash) {
        try {
            String token = computadoraService.generarTokenAgente(id, actor.getUsuarioId(), actor.getCorreo());
            flash.addFlashAttribute("tokenAgente", token);
            flash.addFlashAttribute("exito", "Token del agente generado. Cópialo ahora: no se vuelve a mostrar.");
        } catch (ReglaNegocioException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/web/admin/computadoras/" + id;
    }

    @PostMapping("/{id}/estado")
    public String cambiarEstado(@PathVariable Long id,
                                @RequestParam boolean activa,
                                @AuthenticationPrincipal PrincipalPortal actor,
                                RedirectAttributes flash) {
        try {
            computadoraService.cambiarEstado(id, activa, actor.getUsuarioId(), actor.getCorreo());
            flash.addFlashAttribute("exito", activa ? "Computadora reactivada." : "Computadora dada de baja.");
        } catch (ReglaNegocioException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/web/admin/computadoras/" + id;
    }
}
