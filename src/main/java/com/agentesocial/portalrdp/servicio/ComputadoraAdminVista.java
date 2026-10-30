package com.agentesocial.portalrdp.servicio;

import com.agentesocial.portalrdp.dominio.EstadoComputadora;

/** Fila de la lista de computadoras (admin): identidad + configuración + operación. */
public record ComputadoraAdminVista(
        Long id,
        String nombre,
        String direccionTailscale,
        String departamentoNombre,
        EstadoComputadora estado,
        String usuarioEnUso,
        String idleTexto,
        String ultimoLatidoTexto,
        boolean credencialConfigurada,
        boolean activa) {

    public boolean esLibre() {
        return estado == EstadoComputadora.LIBRE;
    }

    public boolean esOcupada() {
        return estado == EstadoComputadora.OCUPADA;
    }

    public boolean esSinConexion() {
        return estado == EstadoComputadora.SIN_CONEXION;
    }
}
