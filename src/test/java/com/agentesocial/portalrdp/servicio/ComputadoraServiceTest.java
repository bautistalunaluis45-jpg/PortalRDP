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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.agentesocial.portalrdp.dominio.Computadora;
import com.agentesocial.portalrdp.dominio.Departamento;
import com.agentesocial.portalrdp.excepcion.ReglaNegocioException;
import com.agentesocial.portalrdp.infraestructura.Cifrador;
import com.agentesocial.portalrdp.infraestructura.PropiedadesEstado;
import com.agentesocial.portalrdp.repositorio.ComputadoraRepository;
import com.agentesocial.portalrdp.repositorio.DepartamentoRepository;

@ExtendWith(MockitoExtension.class)
class ComputadoraServiceTest {

    @Mock
    private ComputadoraRepository computadoraRepository;
    @Mock
    private DepartamentoRepository departamentoRepository;
    @Mock
    private Cifrador cifrador;
    @Mock
    private AuditoriaService auditoriaService;
    @Mock
    private com.agentesocial.portalrdp.repositorio.SesionRdpRepository sesionRdpRepository;
    @Mock
    private com.agentesocial.portalrdp.repositorio.UsuarioRepository usuarioRepository;

    private ComputadoraService servicio;
    private Departamento soporte;

    @BeforeEach
    void preparar() {
        servicio = new ComputadoraService(computadoraRepository, departamentoRepository, cifrador,
                Clock.fixed(Instant.parse("2026-10-30T12:00:00Z"), ZoneOffset.UTC),
                new PropiedadesEstado(), auditoriaService, sesionRdpRepository, usuarioRepository);
        soporte = new Departamento("Soporte", "SOP", null);
        ReflectionTestUtils.setField(soporte, "id", 10L);
    }

    @Test
    void crearCifraLaContrasenaYAudita() {
        when(computadoraRepository.existsByNombreIgnoreCase("PC-01")).thenReturn(false);
        when(computadoraRepository.existsByDireccionTailscaleIgnoreCase("100.100.0.9")).thenReturn(false);
        when(departamentoRepository.findById(10L)).thenReturn(Optional.of(soporte));
        when(cifrador.cifrar("secreta")).thenReturn(new byte[]{1, 2, 3});

        servicio.crear("PC-01", "100.100.0.9", "rdpuser", 10L, "secreta", 1L, "admin@x.com");

        verify(cifrador).cifrar("secreta");
        verify(computadoraRepository).save(any(Computadora.class));
        verify(auditoriaService).cambioComputadora(eq(1L), eq("admin@x.com"), any(), anyString());
    }

    @Test
    void crearExigeContrasena() {
        assertThatThrownBy(() -> servicio.crear("PC-01", "100.100.0.9", "rdpuser", 10L, "  ", 1L, "a@x.com"))
                .isInstanceOf(ReglaNegocioException.class);
        verify(computadoraRepository, never()).save(any());
    }

    @Test
    void crearRechazaNombreDuplicado() {
        when(computadoraRepository.existsByNombreIgnoreCase("PC-01")).thenReturn(true);
        assertThatThrownBy(() -> servicio.crear("PC-01", "100.100.0.9", "rdpuser", 10L, "secreta", 1L, "a@x.com"))
                .isInstanceOf(ReglaNegocioException.class);
        verify(computadoraRepository, never()).save(any());
    }

    @Test
    void actualizarSinContrasenaNoRecifra() {
        Computadora c = new Computadora("PC-01", soporte, "100.100.0.9");
        ReflectionTestUtils.setField(c, "id", 5L);
        when(computadoraRepository.findById(5L)).thenReturn(Optional.of(c));
        when(departamentoRepository.findById(10L)).thenReturn(Optional.of(soporte));

        servicio.actualizar(5L, "PC-01b", "100.100.0.9", "rdpuser", 10L, "   ", 1L, "a@x.com");

        verify(cifrador, never()).cifrar(anyString());
        verify(auditoriaService).cambioComputadora(anyLong(), anyString(), eq(5L), anyString());
        verify(auditoriaService, never()).cambioPasswordPc(anyLong(), anyString(), anyLong());
    }

    @Test
    void actualizarConContrasenaRecifraYAudita() {
        Computadora c = new Computadora("PC-01", soporte, "100.100.0.9");
        ReflectionTestUtils.setField(c, "id", 5L);
        when(computadoraRepository.findById(5L)).thenReturn(Optional.of(c));
        when(departamentoRepository.findById(10L)).thenReturn(Optional.of(soporte));
        when(cifrador.cifrar("nueva")).thenReturn(new byte[]{9});

        servicio.actualizar(5L, "PC-01", "100.100.0.9", "rdpuser", 10L, "nueva", 1L, "a@x.com");

        verify(cifrador).cifrar("nueva");
        verify(auditoriaService).cambioPasswordPc(eq(1L), eq("a@x.com"), eq(5L));
        assertThat(c.getCredencialCifrada()).containsExactly((byte) 9);
    }

    @Test
    void darDeBajaDesactivaYAudita() {
        Computadora c = new Computadora("PC-01", soporte, "100.100.0.9");
        ReflectionTestUtils.setField(c, "id", 5L);
        when(computadoraRepository.findById(5L)).thenReturn(Optional.of(c));

        servicio.cambiarEstado(5L, false, 1L, "a@x.com");

        assertThat(c.isActiva()).isFalse();
        verify(auditoriaService).cambioComputadora(anyLong(), anyString(), eq(5L), anyString());
    }
}
