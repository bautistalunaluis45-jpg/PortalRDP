package com.agentesocial.portalrdp.servicio;

import com.agentesocial.portalrdp.dominio.EstadoComputadora;

/**
 * Datos de una computadora para la pantalla de edición. NO incluye la contraseña
 * (nunca se muestra); solo si ya hay una credencial configurada.
 */
public record ComputadoraDetalle(
        Long id,
        String nombre,
        String direccionTailscale,
        String cuentaUsuario,
        Long departamentoId,
        String departamentoNombre,
        EstadoComputadora estado,
        String ultimoLatidoTexto,
        boolean credencialConfigurada,
        boolean tokenConfigurado,
        boolean activa) {

    public boolean esSinConexion() {
        return estado == EstadoComputadora.SIN_CONEXION;
    }
}
