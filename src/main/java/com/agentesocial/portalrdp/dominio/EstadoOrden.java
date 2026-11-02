package com.agentesocial.portalrdp.dominio;

/** Ciclo de vida de una orden hacia el agente. */
public enum EstadoOrden {
    /** Creada, esperando a que el agente la recoja en su próximo latido. */
    PENDIENTE,
    /** Entregada al agente (va en la respuesta de un latido). */
    ENTREGADA,
    /** El agente la ejecutó correctamente. */
    EJECUTADA,
    /** El agente la intentó y falló (p. ej. la sesión ya no coincide). */
    FALLIDA,
    /** Venció sin que el agente la recogiera. */
    EXPIRADA
}
