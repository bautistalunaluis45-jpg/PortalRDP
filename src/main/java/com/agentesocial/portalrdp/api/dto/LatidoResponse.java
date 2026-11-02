package com.agentesocial.portalrdp.api.dto;

import java.util.List;

/**
 * Respuesta al latido: confirmación, cuántas sesiones quedaron abiertas y las órdenes
 * pendientes que el agente debe ejecutar (lista vacía si no hay).
 */
public record LatidoResponse(boolean ok, int sesionesAbiertas, List<OrdenDto> ordenes) {
}
