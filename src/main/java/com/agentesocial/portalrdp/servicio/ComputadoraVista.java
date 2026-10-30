package com.agentesocial.portalrdp.servicio;

import com.agentesocial.portalrdp.dominio.EstadoComputadora;

/**
 * Datos de una computadora listos para la pantalla "Mis computadoras".
 * No expone la entidad. {@code usuarioEnUso} y {@code etiquetaVigencia} pueden ser nulos.
 */
public record ComputadoraVista(
        Long id,
        String nombre,
        EstadoComputadora estado,
        String usuarioEnUso,
        boolean temporal,
        String etiquetaVigencia) {

    public boolean esLibre() {
        return estado == EstadoComputadora.LIBRE;
    }

    public boolean esOcupada() {
        return estado == EstadoComputadora.OCUPADA;
    }

    public boolean esSinConexion() {
        return estado == EstadoComputadora.SIN_CONEXION;
    }

    public boolean puedeConectar() {
        return esLibre();
    }
}
