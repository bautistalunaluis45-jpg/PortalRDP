package com.agentesocial.portalrdp.dominio;

/** Estado de un token de lanzamiento (docs/decisiones.md 12). */
public enum EstadoLanzamiento {
    /** Emitido, aún no canjeado por el helper. */
    EMITIDO,
    /** El helper lo canjeó (recibió host/credencial/preferencias). De un solo uso. */
    CANJEADO,
    /** Venció sin canjearse. */
    EXPIRADO,
    /** La sesión asociada terminó (marcado a futuro por el agente). */
    TERMINADO
}
