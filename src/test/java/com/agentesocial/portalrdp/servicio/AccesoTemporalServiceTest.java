package com.agentesocial.portalrdp.servicio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.agentesocial.portalrdp.dominio.AccesoTemporal;
import com.agentesocial.portalrdp.dominio.Computadora;
import com.agentesocial.portalrdp.dominio.Departamento;
import com.agentesocial.portalrdp.dominio.Rol;
import com.agentesocial.portalrdp.dominio.Usuario;
import com.agentesocial.portalrdp.excepcion.ReglaNegocioException;
import com.agentesocial.portalrdp.repositorio.AccesoTemporalRepository;
import com.agentesocial.portalrdp.repositorio.ComputadoraRepository;
import com.agentesocial.portalrdp.repositorio.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class AccesoTemporalServiceTest {

    private static final Instant AHORA = Instant.parse("2026-10-30T12:00:00Z");

    @Mock
    private AccesoTemporalRepository accesoTemporalRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private ComputadoraRepository computadoraRepository;
    @Mock
    private AuditoriaService auditoriaService;

    private AccesoTemporalService servicio;
    private Usuario empleado;
    private Computadora pc;

    @BeforeEach
    void preparar() {
        servicio = new AccesoTemporalService(accesoTemporalRepository, usuarioRepository,
                computadoraRepository, Clock.fixed(AHORA, ZoneOffset.UTC), auditoriaService);
        Departamento dep = new Departamento("Soporte", "SOP", null);
        ReflectionTestUtils.setField(dep, "id", 10L);
        empleado = new Usuario("Ana", "ana@healthcarepi.com", dep, Rol.EMPLEADO);
        ReflectionTestUtils.setField(empleado, "id", 2L);
        pc = new Computadora("PC-1", dep, "100.1.1.1");
        ReflectionTestUtils.setField(pc, "id", 5L);
    }

    @Test
    void otorgarGuardaConVencimientoFuturoYAudita() {
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(empleado));
        when(computadoraRepository.findById(5L)).thenReturn(Optional.of(pc));
        when(accesoTemporalRepository.existeVigente(eq(2L), eq(5L), any()))
                .thenReturn(false);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(empleado));

        servicio.otorgar(2L, 5L, 24, "soporte", 1L, "admin@x.com");

        verify(accesoTemporalRepository).save(any(AccesoTemporal.class));
        verify(auditoriaService).accesoTemporalCreado(eq(1L), eq("admin@x.com"), eq(2L), eq(5L), anyString());
    }

    @Test
    void otorgarIndefinidoGuardaSinVencimiento() {
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(empleado));
        when(computadoraRepository.findById(5L)).thenReturn(Optional.of(pc));
        when(accesoTemporalRepository.existeVigente(eq(2L), eq(5L), any())).thenReturn(false);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(empleado));

        servicio.otorgar(2L, 5L, null, null, 1L, "admin@x.com");

        ArgumentCaptor<AccesoTemporal> captor = ArgumentCaptor.forClass(AccesoTemporal.class);
        verify(accesoTemporalRepository).save(captor.capture());
        assertThat(captor.getValue().getVenceEn()).isNull();
    }

    @Test
    void otorgarRechazaSiYaHayAccesoVigente() {
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(empleado));
        when(computadoraRepository.findById(5L)).thenReturn(Optional.of(pc));
        when(accesoTemporalRepository.existeVigente(eq(2L), eq(5L), any()))
                .thenReturn(true);

        assertThatThrownBy(() -> servicio.otorgar(2L, 5L, 24, null, 1L, "a@x.com"))
                .isInstanceOf(ReglaNegocioException.class);
        verify(accesoTemporalRepository, never()).save(any());
    }

    @Test
    void otorgarRechazaDuracionCero() {
        assertThatThrownBy(() -> servicio.otorgar(2L, 5L, 0, null, 1L, "a@x.com"))
                .isInstanceOf(ReglaNegocioException.class);
        verify(accesoTemporalRepository, never()).save(any());
    }

    @Test
    void revocarMarcaRevocadoEnYAudita() {
        AccesoTemporal acceso = new AccesoTemporal(empleado, pc, AHORA.plusSeconds(3600), empleado, "x");
        ReflectionTestUtils.setField(acceso, "id", 77L);
        when(accesoTemporalRepository.findById(77L)).thenReturn(Optional.of(acceso));

        servicio.revocar(77L, 1L, "admin@x.com");

        assertThat(acceso.getRevocadoEn()).isEqualTo(AHORA);
        verify(auditoriaService).accesoTemporalRevocado(eq(1L), eq("admin@x.com"), eq(2L), eq(5L), anyString());
    }
}
