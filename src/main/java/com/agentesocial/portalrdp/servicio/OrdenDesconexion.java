package com.agentesocial.portalrdp.servicio;

/** Resultado de encolar una desconexión: para el mensaje de confirmación en la vista. */
public record OrdenDesconexion(String computadoraNombre, String usuario) {
}
