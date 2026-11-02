package com.agentesocial.portalrdp.api.dto;

/** Acuse del agente tras ejecutar una orden: si salió bien y un detalle breve. */
public record OrdenResultadoRequest(boolean ok, String detalle) {
}
