package com.agentesocial.portalrdp.servicio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.agentesocial.portalrdp.dominio.EventoAuditoria;
import com.agentesocial.portalrdp.dominio.TipoEventoAuditoria;
import com.agentesocial.portalrdp.repositorio.EventoAuditoriaRepository;

@ExtendWith(MockitoExtension.class)
class AuditoriaServiceTest {

    private static final Instant AHORA = Instant.parse("2026-10-29T18:00:00Z");

    @Mock
    private EventoAuditoriaRepository repositorio;
    @Captor
    private ArgumentCaptor<EventoAuditoria> captor;

    @Test
    void registraCambioDepartamentoConLosDatosYLaHoraDelReloj() {
        AuditoriaService servicio = new AuditoriaService(repositorio, Clock.fixed(AHORA, ZoneOffset.UTC));

        servicio.cambioDepartamento(7L, "admin@healthcarepi.com", "Soporte", "Renombrado: Soporte -> Mesa de ayuda");

        verify(repositorio).save(captor.capture());
        EventoAuditoria evento = captor.getValue();
        assertThat(evento.getTipo()).isEqualTo(TipoEventoAuditoria.CAMBIO_DEPARTAMENTO);
        assertThat(evento.getOcurridoEn()).isEqualTo(AHORA);
        assertThat(evento.getActorUsuarioId()).isEqualTo(7L);
        assertThat(evento.getActorCorreo()).isEqualTo("admin@healthcarepi.com");
        assertThat(evento.getDepartamentoNombre()).isEqualTo("Soporte");
        assertThat(evento.getDetalle()).contains("Mesa de ayuda");
    }
}
