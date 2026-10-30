package com.agentesocial.portalrdp.dominio;

/**
 * Estado calculado de una computadora (no se persiste). Ver docs/decisiones.md
 * seccion 12 y 6.
 * - LIBRE: reporta latido reciente y no tiene sesion conectada.
 * - OCUPADA: tiene una sesion conectada (dato del agente; se habilita en fase 4).
 * - SIN_CONEXION: no reporta latido dentro del umbral (PC apagada o agente caido).
 */
public enum EstadoComputadora {
    LIBRE,
    OCUPADA,
    SIN_CONEXION
}
