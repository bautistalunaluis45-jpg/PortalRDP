package com.agentesocial.portalrdp.dominio;

/** Órdenes que el portal puede mandar al agente de una PC (lista blanca). */
public enum TipoOrden {
    /** Desconectar una sesión (sin cerrarla): tsdiscon / WTSDisconnectSession. */
    DESCONECTAR
}
