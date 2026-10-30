package com.agentesocial.portalrdp.api.dto;

import java.time.Instant;

/**
 * Una sesión de Windows reportada por el agente en el latido.
 * {@code estado} = "ACTIVA" o "DESCONECTADA". {@code desde} opcional (si falta, se usa el momento del latido).
 */
public record SesionDto(
        String idSesionWindows,
        String estado,
        String usuarioWindows,
        String ipOrigen,
        Integer idleSegundos,
        Instant desde) {
}
