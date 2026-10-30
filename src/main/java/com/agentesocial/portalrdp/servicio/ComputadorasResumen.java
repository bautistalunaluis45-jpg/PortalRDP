package com.agentesocial.portalrdp.servicio;

/** Conteos para los indicadores de la pantalla de computadoras. */
public record ComputadorasResumen(
        long total, long libres, long ocupadas, long sinConexion, long sinCredencial) {
}
