package com.agentesocial.portalrdp.api.dto;

import java.util.List;

/** Cuerpo del latido del agente: las sesiones de Windows actuales de la PC. */
public record LatidoRequest(List<SesionDto> sesiones) {
}
