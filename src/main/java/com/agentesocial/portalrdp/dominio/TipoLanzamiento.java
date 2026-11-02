package com.agentesocial.portalrdp.dominio;

/** Qué va a hacer el helper al canjear el token. */
public enum TipoLanzamiento {
    /** Abrir una sesión RDP normal (mstsc con credencial). */
    CONECTAR,
    /** Observar una sesión existente (mstsc /shadow, sin credencial). */
    SHADOW
}
