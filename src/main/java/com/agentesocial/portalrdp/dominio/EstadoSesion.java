package com.agentesocial.portalrdp.dominio;

/** Estado de una sesión de Windows reportada por el agente (docs/decisiones.md 12). */
public enum EstadoSesion {
    /** Conectada (alguien está usando la PC ahora). */
    ACTIVA,
    /** Desconectada pero la sesión sigue abierta en la PC (nadie conectado). */
    DESCONECTADA
}
