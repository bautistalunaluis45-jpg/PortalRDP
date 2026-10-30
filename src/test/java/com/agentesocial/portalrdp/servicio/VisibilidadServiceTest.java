package com.agentesocial.portalrdp.servicio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.agentesocial.portalrdp.dominio.AccesoTemporal;
import com.agentesocial.portalrdp.dominio.Computadora;
import com.agentesocial.portalrdp.dominio.Departamento;
import com.agentesocial.portalrdp.dominio.Rol;
import com.agentesocial.portalrdp.dominio.Usuario;
import com.agentesocial.portalrdp.infraestructura.PropiedadesEstado;
import com.agentesocial.portalrdp.repositorio.AccesoTemporalRepository;
import com.agentesocial.portalrdp.repositorio.ComputadoraRepository;
import com.agentesocial.portalrdp.repositorio.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class VisibilidadServiceTest {

    private static final Instant AHORA = Instant.parse("2026-10-29T12:00:00Z");

    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private ComputadoraRepository computadoraRepository;
    @Mock
    private AccesoTemporalRepository accesoTemporalRepository;
    @Mock
    private com.agentesocial.portalrdp.repositorio.SesionRdpRepository sesionRdpRepository;

    private VisibilidadService servicio;
    private Usuario empleado;
    private Departamento soporte;

    @BeforeEach
    void preparar() {
        Clock relojFijo = Clock.fixed(AHORA, ZoneOffset.UTC);
        PropiedadesEstado propiedades = new PropiedadesEstado(); // umbral por defecto: 90 s
        servicio = new VisibilidadService(usuarioRepository, computadoraRepository,
                accesoTemporalRepository, sesionRdpRepository, relojFijo, propiedades);

        soporte = new Departamento("Soporte", "SOP", null);
        asignarId(soporte, 10L);
        empleado = new Usuario("Empleado", "empleado@healthcarepi.com", soporte, Rol.EMPLEADO);
        asignarId(empleado, 1L);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(empleado));
    }

    private static void asignarId(Object entidad, Long id) {
        ReflectionTestUtils.setField(entidad, "id", id);
    }

    private Computadora pc(String nombre, Long id, Instant ultimoLatido, Departamento depto) {
        Computadora c = new Computadora(nombre, depto, "100.100.0." + id);
        asignarId(c, id);
        c.setUltimoLatido(ultimoLatido);
        return c;
    }

    @Test
    void calculaLibreYSinConexionYCuenta() {
        Computadora conLatido = pc("PC-SOPORTE-01", 100L, AHORA, soporte);
        Computadora sinLatido = pc("PC-SOPORTE-02", 101L, null, soporte);
        when(computadoraRepository.findByDepartamento_IdAndActivaTrueOrderByNombreAsc(any()))
                .thenReturn(List.of(conLatido, sinLatido));

        Departamento ventas = new Departamento("Ventas", "VEN", null);
        asignarId(ventas, 20L);
        Computadora pcVentas = pc("PC-VENTAS-01", 200L, AHORA, ventas);
        AccesoTemporal acceso = new AccesoTemporal(empleado, pcVentas,
                AHORA.plusSeconds(3600), empleado, "demo");
        when(accesoTemporalRepository.vigentesDeUsuario(any(), any()))
                .thenReturn(List.of(acceso));

        MisComputadorasVista vista = servicio.misComputadoras(1L);

        assertThat(vista.departamentoNombre()).isEqualTo("Soporte");
        assertThat(vista.departamento()).hasSize(2);
        assertThat(vista.departamento().get(0).esLibre()).isTrue();
        assertThat(vista.departamento().get(1).esSinConexion()).isTrue();

        assertThat(vista.temporales()).hasSize(1);
        ComputadoraVista temporal = vista.temporales().get(0);
        assertThat(temporal.temporal()).isTrue();
        assertThat(temporal.etiquetaVigencia()).startsWith("Vence");
        assertThat(temporal.usuarioEnUso()).isNull();

        assertThat(vista.total()).isEqualTo(3);
        assertThat(vista.libres()).isEqualTo(2);
        assertThat(vista.sinConexion()).isEqualTo(1);
        assertThat(vista.ocupadas()).isZero();
    }

    @Test
    void latidoMasViejoQueElUmbralEsSinConexion() {
        Computadora vieja = pc("PC-SOPORTE-09", 109L, AHORA.minusSeconds(120), soporte);
        when(computadoraRepository.findByDepartamento_IdAndActivaTrueOrderByNombreAsc(any()))
                .thenReturn(List.of(vieja));
        when(accesoTemporalRepository.vigentesDeUsuario(any(), any()))
                .thenReturn(List.of());

        MisComputadorasVista vista = servicio.misComputadoras(1L);

        assertThat(vista.departamento().get(0).esSinConexion()).isTrue();
        assertThat(vista.sinConexion()).isEqualTo(1);
    }

    @Test
    void noDuplicaUnaPcQueEstaEnDepartamentoYEnAccesoTemporal() {
        Computadora compartida = pc("PC-SOPORTE-01", 100L, AHORA, soporte);
        when(computadoraRepository.findByDepartamento_IdAndActivaTrueOrderByNombreAsc(any()))
                .thenReturn(List.of(compartida));

        AccesoTemporal acceso = new AccesoTemporal(empleado, compartida,
                AHORA.plusSeconds(3600), empleado, "demo");
        when(accesoTemporalRepository.vigentesDeUsuario(any(), any()))
                .thenReturn(List.of(acceso));

        MisComputadorasVista vista = servicio.misComputadoras(1L);

        assertThat(vista.departamento()).hasSize(1);
        assertThat(vista.temporales()).isEmpty();
        assertThat(vista.total()).isEqualTo(1);
    }
}
