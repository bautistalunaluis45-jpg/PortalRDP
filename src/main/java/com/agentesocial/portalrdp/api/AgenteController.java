package com.agentesocial.portalrdp.api;

import java.nio.file.Files;
import java.nio.file.Path;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.agentesocial.portalrdp.api.dto.LatidoRequest;
import com.agentesocial.portalrdp.api.dto.LatidoResponse;
import com.agentesocial.portalrdp.api.dto.OrdenResultadoRequest;
import com.agentesocial.portalrdp.excepcion.AgenteNoAutorizadoException;
import com.agentesocial.portalrdp.excepcion.ReglaNegocioException;
import com.agentesocial.portalrdp.infraestructura.PropiedadesAgente;
import com.agentesocial.portalrdp.servicio.AgenteService;

/**
 * API del agente (.NET en cada PC). Autenticación por token por PC en la cabecera
 * X-Agent-Token (latido y acuse). Cadena de seguridad separada (sin Google, sin CSRF,
 * sin sesión). La descarga del instalador NO requiere token: el binario no tiene secretos
 * y el acceso ya está limitado a la red privada (Tailscale).
 */
@RestController
@RequestMapping("/api/agente")
public class AgenteController {

    private final AgenteService agenteService;
    private final PropiedadesAgente propiedadesAgente;

    public AgenteController(AgenteService agenteService, PropiedadesAgente propiedadesAgente) {
        this.agenteService = agenteService;
        this.propiedadesAgente = propiedadesAgente;
    }

    @PostMapping("/latido")
    public ResponseEntity<LatidoResponse> latido(
            @RequestHeader(value = "X-Agent-Token", required = false) String token,
            @RequestBody(required = false) LatidoRequest request) {
        try {
            return ResponseEntity.ok(agenteService.procesarLatido(token, request));
        } catch (AgenteNoAutorizadoException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    /** Acuse de una orden ejecutada por el agente (p. ej. DESCONECTAR). */
    @PostMapping("/ordenes/{id}/resultado")
    public ResponseEntity<Void> resultadoOrden(
            @RequestHeader(value = "X-Agent-Token", required = false) String token,
            @PathVariable Long id,
            @RequestBody(required = false) OrdenResultadoRequest request) {
        boolean ok = request != null && request.ok();
        String detalle = request == null ? null : request.detalle();
        try {
            agenteService.registrarResultadoOrden(token, id, ok, detalle);
            return ResponseEntity.ok().build();
        } catch (AgenteNoAutorizadoException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        } catch (ReglaNegocioException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /** Descarga del instalador del agente SIN sesión (dentro del tailnet). 404 si no está publicado. */
    @GetMapping("/descargar")
    public ResponseEntity<Resource> descargar() {
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
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + archivo.getFileName() + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .contentLength(tamano)
                .body(new FileSystemResource(archivo));
    }
}
