package com.agentesocial.portalrdp.servicio;

/**
 * Lo que el helper recibe al canjear el token (única salida de la contraseña del portal).
 * Para SHADOW, {@code password} es null y {@code idSesionObjetivo} trae la sesión a observar.
 * Las preferencias ya vienen aplanadas para que el helper arme el .rdp.
 */
public record CanjeResultado(
        String tipo,
        String host,
        String cuenta,
        String password,
        String idSesionObjetivo,
        String computadoraNombre,
        boolean ajusteAutomatico,
        boolean pantallaCompleta,
        boolean multimonitor,
        int escala,
        String sonido,
        boolean microfono,
        String atajos,
        String calidad) {
}
