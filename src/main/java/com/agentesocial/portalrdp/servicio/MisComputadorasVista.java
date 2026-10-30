package com.agentesocial.portalrdp.servicio;

import java.util.List;

/** Modelo completo de la pantalla "Mis computadoras" para un usuario. */
public record MisComputadorasVista(
        String departamentoNombre,
        List<ComputadoraVista> departamento,
        List<ComputadoraVista> temporales,
        int total,
        int libres,
        int ocupadas,
        int sinConexion) {
}
