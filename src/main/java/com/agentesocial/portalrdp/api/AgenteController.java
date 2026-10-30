package com.agentesocial.portalrdp.api;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.agentesocial.portalrdp.api.dto.LatidoRequest;
import com.agentesocial.portalrdp.api.dto.LatidoResponse;
import com.agentesocial.portalrdp.excepcion.AgenteNoAutorizadoException;
import com.agentesocial.portalrdp.servicio.AgenteService;

/**
 * API del agente (.NET en cada PC). Autenticación por token por PC en la cabecera
 * X-Agent-Token. Cadena de seguridad separada (sin Google, sin CSRF, sin sesión).
 */
@RestController
@RequestMapping("/api/agente")
public class AgenteController {

    private final AgenteService agenteService;

    public AgenteController(AgenteService agenteService) {
        this.agenteService = agenteService;
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
}
