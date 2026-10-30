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

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.agentesocial.portalrdp.dominio.Departamento;
import com.agentesocial.portalrdp.excepcion.ReglaNegocioException;
import com.agentesocial.portalrdp.repositorio.ComputadoraRepository;
import com.agentesocial.portalrdp.repositorio.DepartamentoRepository;
import com.agentesocial.portalrdp.repositorio.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class DepartamentoServiceTest {

    @Mock
    private DepartamentoRepository departamentoRepository;
    @Mock
    private ComputadoraRepository computadoraRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private AuditoriaService auditoriaService;

    private DepartamentoService servicio() {
        return new DepartamentoService(departamentoRepository, computadoraRepository,
                usuarioRepository, auditoriaService);
    }

    @Test
    void crearGuardaYRegistraAuditoria() {
        when(departamentoRepository.existsByNombreIgnoreCase("Soporte")).thenReturn(false);
        when(departamentoRepository.existsByCodigoIgnoreCase("SOP")).thenReturn(false);

        servicio().crear("  Soporte ", " SOP ", "  ", 1L, "admin@x.com");

        verify(departamentoRepository).save(any(Departamento.class));
        verify(auditoriaService).cambioDepartamento(eq(1L), eq("admin@x.com"), eq("Soporte"), anyString());
    }

    @Test
    void crearRechazaNombreDuplicado() {
        when(departamentoRepository.existsByNombreIgnoreCase("Soporte")).thenReturn(true);

        assertThatThrownBy(() -> servicio().crear("Soporte", "SOP", null, 1L, "a@x.com"))
                .isInstanceOf(ReglaNegocioException.class);
        verify(departamentoRepository, never()).save(any());
    }

    @Test
    void crearRechazaCodigoVacio() {
        assertThatThrownBy(() -> servicio().crear("Soporte", "   ", null, 1L, "a@x.com"))
                .isInstanceOf(ReglaNegocioException.class);
        verify(departamentoRepository, never()).save(any());
    }

    @Test
    void eliminarRechazaSiTienePcsOUsuarios() {
        Departamento d = new Departamento("Soporte", "SOP", null);
        ReflectionTestUtils.setField(d, "id", 5L);
        when(departamentoRepository.findById(5L)).thenReturn(Optional.of(d));
        when(computadoraRepository.countByDepartamento_Id(5L)).thenReturn(3L);

        assertThatThrownBy(() -> servicio().eliminar(5L, 1L, "a@x.com"))
                .isInstanceOf(ReglaNegocioException.class);
        verify(departamentoRepository, never()).delete(any());
    }

    @Test
    void eliminarOkCuandoEstaVacio() {
        Departamento d = new Departamento("Legal", "LEG", null);
        ReflectionTestUtils.setField(d, "id", 9L);
        when(departamentoRepository.findById(9L)).thenReturn(Optional.of(d));
        when(computadoraRepository.countByDepartamento_Id(9L)).thenReturn(0L);
        when(usuarioRepository.countByDepartamento_Id(9L)).thenReturn(0L);

        servicio().eliminar(9L, 1L, "a@x.com");

        verify(departamentoRepository).delete(d);
        verify(auditoriaService).cambioDepartamento(anyLong(), anyString(), eq("Legal"), anyString());
    }
}
