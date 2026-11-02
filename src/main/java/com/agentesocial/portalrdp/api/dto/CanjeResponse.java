package com.agentesocial.portalrdp.api.dto;

/**
 * Respuesta del canje hacia el helper. Para SHADOW, {@code password} es null y
 * {@code idSesionObjetivo} trae la sesión a observar. Preferencias aplanadas para el .rdp.
 */
public record CanjeResponse(
        String tipo,
        String host,
        String cuenta,
        String password,
        String idSesionObjetivo,
        String computadora,
        boolean ajusteAutomatico,
        boolean pantallaCompleta,
        boolean multimonitor,
        int escala,
        String sonido,
        boolean microfono,
        String atajos,
        String calidad) {
}
