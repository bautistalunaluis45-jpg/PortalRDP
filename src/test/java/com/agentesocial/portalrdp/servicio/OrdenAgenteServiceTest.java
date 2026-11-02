package com.agentesocial.portalrdp.servicio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.agentesocial.portalrdp.dominio.Computadora;
import com.agentesocial.portalrdp.dominio.EstadoSesion;
import com.agentesocial.portalrdp.dominio.OrdenAgente;
import com.agentesocial.portalrdp.dominio.SesionRdp;
import com.agentesocial.portalrdp.dominio.Usuario;
import com.agentesocial.portalrdp.excepcion.ReglaNegocioException;
import com.agentesocial.portalrdp.repositorio.ComputadoraRepository;
import com.agentesocial.portalrdp.repositorio.OrdenAgenteRepository;
import com.agentesocial.portalrdp.repositorio.SesionRdpRepository;
import com.agentesocial.portalrdp.repositorio.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class OrdenAgenteServiceTest {

    private static final Instant AHORA = Instant.parse("2026-01-01T12:00:00Z");

    @Mock private OrdenAgenteRepository ordenRepository;
    @Mock private ComputadoraRepository computadoraRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private SesionRdpRepository sesionRepository;
    @Mock private AuditoriaService auditoriaService;

    private final Clock reloj = Clock.fixed(AHORA, ZoneOffset.UTC);

    private OrdenAgenteService servicio() {
        return new OrdenAgenteService(ordenRepository, computadoraRepository, usuarioRepository,
                sesionRepository, auditoriaService, reloj);
    }

    @Test
    void desconectarConSesionActiva_encolaYaudita() {
        Computadora c = mock(Computadora.class);
        when(c.getNombre()).thenReturn("WS-5");
        SesionRdp s = mock(SesionRdp.class);
        when(s.getIdSesionWindows()).thenReturn("4");
        when(s.getUsuarioWindows()).thenReturn("DOM\\ana");
        when(computadoraRepository.findById(5L)).thenReturn(Optional.of(c));
        when(sesionRepository.findFirstByComputadora_IdAndEstadoAndFinEnIsNull(5L, EstadoSesion.ACTIVA))
                .thenReturn(Optional.of(s));
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(mock(Usuario.class)));

        OrdenDesconexion r = servicio().ordenarDesconectar(2L, "m@x.com", 5L);

        assertThat(r.computadoraNombre()).isEqualTo("WS-5");
        assertThat(r.usuario()).isEqualTo("DOM\\ana");
        verify(ordenRepository).save(any(OrdenAgente.class));
        verify(auditoriaService).desconectar(eq(2L), eq("m@x.com"), eq(5L), contains("ana"));
    }

    @Test
    void desconectarSinSesionActiva_rechaza() {
        when(computadoraRepository.findById(5L)).thenReturn(Optional.of(mock(Computadora.class)));
        when(sesionRepository.findFirstByComputadora_IdAndEstadoAndFinEnIsNull(5L, EstadoSesion.ACTIVA))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio().ordenarDesconectar(2L, "m@x.com", 5L))
                .isInstanceOf(ReglaNegocioException.class);
        verify(ordenRepository, never()).save(any());
        verify(auditoriaService, never()).desconectar(any(), anyString(), any(), anyString());
    }
}
