package com.agentesocial.portalrdp.servicio;

/** Fila de la pantalla de departamentos (A4), con conteos reales. */
public record DepartamentoVista(
        Long id,
        String nombre,
        String codigo,
        String descripcion,
        long cantidadPcs,
        long cantidadUsuarios) {

    /** Solo se puede borrar un departamento sin PCs ni usuarios. */
    public boolean eliminable() {
        return cantidadPcs == 0 && cantidadUsuarios == 0;
    }
}
