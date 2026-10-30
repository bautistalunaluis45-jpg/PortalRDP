package com.agentesocial.portalrdp.servicio;

import java.time.Duration;
import java.time.Instant;

import com.agentesocial.portalrdp.dominio.EstadoComputadora;

/**
 * Cálculo del estado de una computadora a partir del último latido.
 * OCUPADA requiere datos de sesión del agente (fase posterior): mientras no
 * existan, {@code ocupada} llega en false y una PC con latido reciente es LIBRE.
 */
public final class EstadoCalculo {

    private EstadoCalculo() {
    }

    public static EstadoComputadora calcular(Instant ultimoLatido, Instant ahora, Duration umbral, boolean ocupada) {
        if (ultimoLatido == null || ultimoLatido.isBefore(ahora.minus(umbral))) {
            return EstadoComputadora.SIN_CONEXION;
        }
        return ocupada ? EstadoComputadora.OCUPADA : EstadoComputadora.LIBRE;
    }
}
