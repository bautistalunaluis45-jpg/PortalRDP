package com.agentesocial.portalrdp.servicio;

/** Una fila de la tabla de auditoría. {@code color} es una clave de color para el punto/etiqueta. */
public record EventoAuditoriaFilaVista(
        String fecha,
        String actor,
        String computadora,
        String tipoEtiqueta,
        String color,
        String detalle,
        String ip) {
}
