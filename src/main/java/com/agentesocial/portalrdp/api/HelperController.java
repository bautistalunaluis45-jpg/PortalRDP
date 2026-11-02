package com.agentesocial.portalrdp.api;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.agentesocial.portalrdp.api.dto.CanjeRequest;
import com.agentesocial.portalrdp.api.dto.CanjeResponse;
import com.agentesocial.portalrdp.excepcion.ReglaNegocioException;
import com.agentesocial.portalrdp.infraestructura.PropiedadesHelper;
import com.agentesocial.portalrdp.servicio.CanjeResultado;
import com.agentesocial.portalrdp.servicio.LanzamientoService;

import jakarta.servlet.http.HttpServletRequest;

/**
 * API del helper (.NET en la PC del operador). Cadena de seguridad separada (sin Google,
 * sin CSRF, sin sesión): para el canje el token es la credencial; version/descarga son
 * públicas dentro del tailnet (el binario no tiene secretos) para la auto-actualización.
 * Ver docs/decisiones.md 5, 19.
 */
@RestController
@RequestMapping("/api/helper")
public class HelperController {

    private final LanzamientoService lanzamientoService;
    private final PropiedadesHelper propiedadesHelper;

    public HelperController(LanzamientoService lanzamientoService, PropiedadesHelper propiedadesHelper) {
        this.lanzamientoService = lanzamientoService;
        this.propiedadesHelper = propiedadesHelper;
    }

    @PostMapping("/canjear")
    public ResponseEntity<?> canjear(@RequestBody(required = false) CanjeRequest request,
                                     HttpServletRequest http) {
        String token = request == null ? null : request.token();
        try {
            CanjeResultado r = lanzamientoService.canjear(token, http.getRemoteAddr());
            return ResponseEntity.ok(new CanjeResponse(
                    r.tipo(), r.host(), r.cuenta(), r.password(), r.idSesionObjetivo(), r.computadoraNombre(),
                    r.ajusteAutomatico(), r.pantallaCompleta(), r.multimonitor(), r.escala(),
                    r.sonido(), r.microfono(), r.atajos(), r.calidad()));
        } catch (ReglaNegocioException e) {
            // Token inválido, ya usado o vencido: 400 con el motivo (nunca se filtra credencial).
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /** Versión publicada del helper, para que el agente/helper decida si debe auto-actualizarse. */
    @GetMapping("/version")
    public Map<String, Object> version() {
        boolean disponible = StringUtils.hasText(propiedadesHelper.getArchivo());
        return Map.of("version", propiedadesHelper.getVersion(), "disponible", disponible);
    }

    /** Descarga del .exe del helper SIN sesión (para la auto-actualización dentro del tailnet). */
    @GetMapping("/descargar")
    public ResponseEntity<Resource> descargar() {
        String ruta = propiedadesHelper.getArchivo();
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
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + archivo.getFileName() + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .contentLength(tamano)
                .body(new FileSystemResource(archivo));
    }
}
