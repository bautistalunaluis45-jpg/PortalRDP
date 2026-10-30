package com.agentesocial.portalrdp.servicio;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.Test;

class ConsultaAuditoriaRangoTest {

    private static final Instant AHORA = Instant.parse("2026-10-30T12:00:00Z");

    @Test
    void rango24h() {
        assertThat(ConsultaAuditoriaService.desdePorRango("24h", AHORA)).isEqualTo(AHORA.minus(Duration.ofHours(24)));
    }

    @Test
    void rango7dPorDefecto() {
        assertThat(ConsultaAuditoriaService.desdePorRango(null, AHORA)).isEqualTo(AHORA.minus(Duration.ofDays(7)));
        assertThat(ConsultaAuditoriaService.desdePorRango("7d", AHORA)).isEqualTo(AHORA.minus(Duration.ofDays(7)));
    }

    @Test
    void rango30d() {
        assertThat(ConsultaAuditoriaService.desdePorRango("30d", AHORA)).isEqualTo(AHORA.minus(Duration.ofDays(30)));
    }

    @Test
    void rangoTodoSinLimite() {
        assertThat(ConsultaAuditoriaService.desdePorRango("todo", AHORA)).isNull();
    }
}
