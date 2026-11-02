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
import com.agentesocial.portalrdp.dominio.Rol;
import com.agentesocial.portalrdp.dominio.Usuario;
import com.agentesocial.portalrdp.excepcion.AccesoDenegadoException;
import com.agentesocial.portalrdp.excepcion.ReglaNegocioException;
import com.agentesocial.portalrdp.repositorio.DepartamentoRepository;
import com.agentesocial.portalrdp.repositorio.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private DepartamentoRepository departamentoRepository;
    @Mock
    private com.agentesocial.portalrdp.repositorio.ComputadoraRepository computadoraRepository;
    @Mock
    private AuditoriaService auditoriaService;
    @Mock
    private com.agentesocial.portalrdp.repositorio.PreferenciasRdpRepository preferenciasRdpRepository;
    @Mock
    private com.agentesocial.portalrdp.repositorio.AccesoTemporalRepository accesoTemporalRepository;
    @Mock
    private com.agentesocial.portalrdp.repositorio.LanzamientoRepository lanzamientoRepository;
    @Mock
    private com.agentesocial.portalrdp.repositorio.OrdenAgenteRepository ordenAgenteRepository;
    @Mock
    private com.agentesocial.portalrdp.repositorio.SesionRdpRepository sesionRdpRepository;

    private UsuarioService servicio() {
        return new UsuarioService(usuarioRepository, departamentoRepository, computadoraRepository, auditoriaService,
                preferenciasRdpRepository, accesoTemporalRepository, lanzamientoRepository, ordenAgenteRepository,
                sesionRdpRepository);
    }

    private Usuario usuarioActivo() {
        Usuario u = new Usuario("Ana", "ana@healthcarepi.com", null, Rol.EMPLEADO);
        u.setActivo(true);
        return u;
    }

    @Test
    void registraElSubLaPrimeraVez() {
        Usuario usuario = usuarioActivo();
        when(usuarioRepository.findByCorreo("ana@healthcarepi.com")).thenReturn(Optional.of(usuario));

        Usuario resultado = servicio().registrarIngresoGoogle("Ana@HealthcarePI.com", "sub-123");

        assertThat(resultado.getGoogleSub()).isEqualTo("sub-123");
    }

    @Test
    void normalizaElCorreoAMinusculasAlBuscar() {
        when(usuarioRepository.findByCorreo(eq("ana@healthcarepi.com")))
                .thenReturn(Optional.of(usuarioActivo()));

        servicio().registrarIngresoGoogle("ANA@HEALTHCAREPI.COM", "sub-1");
        // Si la normalizacion fallara, el stub de arriba no coincidiria y
        // registrarIngresoGoogle lanzaria AccesoDenegadoException.
    }

    @Test
    void rechazaCuandoNoExisteUsuario() {
        when(usuarioRepository.findByCorreo("nadie@healthcarepi.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio().registrarIngresoGoogle("nadie@healthcarepi.com", "sub"))
                .isInstanceOf(AccesoDenegadoException.class);
    }

    @Test
    void rechazaCuandoElUsuarioEstaInactivo() {
        Usuario inactivo = usuarioActivo();
        inactivo.setActivo(false);
        when(usuarioRepository.findByCorreo("ana@healthcarepi.com")).thenReturn(Optional.of(inactivo));

        assertThatThrownBy(() -> servicio().registrarIngresoGoogle("ana@healthcarepi.com", "sub"))
                .isInstanceOf(AccesoDenegadoException.class);
    }

    // --- Administracion ---

    @Test
    void crearGuardaYAudita() {
        when(usuarioRepository.findByCorreo("ana@healthcarepi.com")).thenReturn(Optional.empty());
        when(departamentoRepository.findById(10L))
                .thenReturn(Optional.of(new Departamento("Soporte", "SOP", null)));

        servicio().crear("Ana López", "ANA@healthcarepi.com", 10L, Rol.EMPLEADO, 1L, "admin@x.com");

        verify(usuarioRepository).save(any(Usuario.class));
        verify(auditoriaService).cambioUsuario(eq(1L), eq("admin@x.com"), any(), anyString());
    }

    @Test
    void crearRechazaCorreoDuplicado() {
        Usuario existente = new Usuario("Otra", "ana@healthcarepi.com", null, Rol.EMPLEADO);
        existente.setActivo(true);
        when(usuarioRepository.findByCorreo("ana@healthcarepi.com")).thenReturn(Optional.of(existente));

        assertThatThrownBy(() -> servicio().crear("Ana", "ana@healthcarepi.com", 10L, Rol.EMPLEADO, 1L, "a@x.com"))
                .isInstanceOf(ReglaNegocioException.class);
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void crearRechazaCorreoConFormatoInvalido() {
        assertThatThrownBy(() -> servicio().crear("Ana", "correo-malo", 10L, Rol.EMPLEADO, 1L, "a@x.com"))
                .isInstanceOf(ReglaNegocioException.class);
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void noPuedeDesactivarseASiMismo() {
        assertThatThrownBy(() -> servicio().cambiarEstado(5L, false, 5L, "yo@x.com"))
                .isInstanceOf(ReglaNegocioException.class);
        verify(usuarioRepository, never()).findById(anyLong());
    }

    @Test
    void noPuedeEliminarseASiMismo() {
        assertThatThrownBy(() -> servicio().eliminar(5L, 5L, "yo@x.com"))
                .isInstanceOf(ReglaNegocioException.class);
        verify(usuarioRepository, never()).findById(anyLong());
        verify(usuarioRepository, never()).delete(any());
    }

    @Test
    void eliminarBorraDatosOperativosAuditaYConservaLaAuditoria() {
        Usuario u = usuarioActivo();
        ReflectionTestUtils.setField(u, "id", 5L);
        when(usuarioRepository.findById(5L)).thenReturn(Optional.of(u));

        servicio().eliminar(5L, 1L, "admin@x.com");

        // Audita el borrado ANTES de borrar (queda constancia con el correo).
        verify(auditoriaService).usuarioEliminado(eq(1L), eq("admin@x.com"), eq(5L), anyString());
        // Limpia lo que lo referencia por FK; las sesiones solo pierden la atribución.
        verify(preferenciasRdpRepository).eliminarPorUsuario(5L);
        verify(accesoTemporalRepository).eliminarPorUsuarioOOtorgante(5L);
        verify(lanzamientoRepository).eliminarPorUsuario(5L);
        verify(ordenAgenteRepository).eliminarPorCreador(5L);
        verify(sesionRdpRepository).desatribuirUsuario(5L);
        verify(usuarioRepository).delete(u);
    }
}
