package com.agentesocial.portalrdp.servicio;

import java.util.List;

/** Página de resultados de la auditoría. */
public record ResultadoAuditoria(
        List<EventoAuditoriaFilaVista> filas,
        int pagina,          // base 0
        int totalPaginas,
        long totalEventos) {

    public int paginaHumana() {
        return pagina + 1;
    }

    public boolean hayAnterior() {
        return pagina > 0;
    }

    public boolean haySiguiente() {
        return pagina + 1 < totalPaginas;
    }
}
