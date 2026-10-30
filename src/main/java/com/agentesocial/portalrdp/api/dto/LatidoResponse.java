package com.agentesocial.portalrdp.api.dto;

/** Respuesta al latido: confirmación y cuántas sesiones quedaron registradas como abiertas. */
public record LatidoResponse(boolean ok, int sesionesAbiertas) {
}
