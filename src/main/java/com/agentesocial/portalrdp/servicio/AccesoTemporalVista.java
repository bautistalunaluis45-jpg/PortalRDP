package com.agentesocial.portalrdp.servicio;

/** Fila de la lista de accesos temporales vigentes (admin). */
public record AccesoTemporalVista(
        Long id,
        String usuarioNombre,
        String usuarioCorreo,
        String iniciales,
        String computadoraNombre,
        String departamentoNombre,
        String venceEnTexto,
        String restanteTexto) {
}
