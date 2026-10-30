package com.agentesocial.portalrdp.servicio;

import java.time.LocalDate;

/**
 * Filtros de la consulta de auditoría. {@code rango} = "24h"/"7d"/"30d"/"todo"/"custom".
 * Con "custom" se usan {@code desde}/{@code hasta} (inclusivas, por día).
 */
public record FiltroAuditoria(String q, Long computadoraId, String tipo, String rango,
                              LocalDate desde, LocalDate hasta) {
}
