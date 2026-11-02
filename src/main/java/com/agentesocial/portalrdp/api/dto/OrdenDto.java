package com.agentesocial.portalrdp.api.dto;

/**
 * Orden que el portal entrega al agente en la respuesta del latido.
 * {@code tipo} = "DESCONECTAR". {@code idSesionObjetivo} = sesión de Windows sobre la que actuar.
 */
public record OrdenDto(Long id, String tipo, String idSesionObjetivo) {
}
