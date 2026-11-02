package com.agentesocial.portalrdp.servicio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
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

import com.agentesocial.portalrdp.dominio.AtajosRdp;
import com.agentesocial.portalrdp.dominio.CalidadRdp;
import com.agentesocial.portalrdp.dominio.Computadora;
import com.agentesocial.portalrdp.dominio.EstadoLanzamiento;
import com.agentesocial.portalrdp.dominio.EstadoSesion;
import com.agentesocial.portalrdp.dominio.Lanzamiento;
import com.agentesocial.portalrdp.dominio.Rol;
import com.agentesocial.portalrdp.dominio.SesionRdp;
import com.agentesocial.portalrdp.dominio.SonidoRdp;
import com.agentesocial.portalrdp.dominio.TipoLanzamiento;
import com.agentesocial.portalrdp.dominio.Usuario;
import com.agentesocial.portalrdp.excepcion.ReglaNegocioException;
import com.agentesocial.portalrdp.infraestructura.Cifrador;
import com.agentesocial.portalrdp.infraestructura.PropiedadesLanzamiento;
import com.agentesocial.portalrdp.repositorio.ComputadoraRepository;
import com.agentesocial.portalrdp.repositorio.LanzamientoRepository;
import com.agentesocial.portalrdp.repositorio.SesionRdpRepository;
import com.agentesocial.portalrdp.repositorio.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class LanzamientoServiceTest {

    private static final Instant AHORA = Instant.parse("2026-01-01T12:00:00Z");

    @Mock private LanzamientoRepository lanzamientoRepository;
    @Mock private ComputadoraRepository computadoraRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private SesionRdpRepository sesionRepository;
    @Mock private VisibilidadService visibilidadService;
    @Mock private AuditoriaService auditoriaService;
    @Mock private PreferenciasService preferenciasService;
    @Mock private Cifrador cifrador;

    private final Clock reloj = Clock.fixed(AHORA, ZoneOffset.UTC);
    private final PropiedadesLanzamiento propiedades = new PropiedadesLanzamiento();

    private LanzamientoService servicio() {
        return new LanzamientoService(lanzamientoRepository, computadoraRepository, usuarioRepository,
                sesionRepository, visibilidadService, auditoriaService, preferenciasService, cifrador,
                reloj, propiedades);
    }

    private Computadora pcActivaConCredencial() {
        Computadora c = mock(Computadora.class);
        when(c.isActiva()).thenReturn(true);
        return c;
    }

    // ---------- emitir ----------

    @Test
    void empleadoConectaPcLibreVisible_emiteToken() {
        Computadora c = pcActivaConCredencial();
        when(c.getCredencialCifrada()).thenReturn(new byte[] {1, 2});
        when(c.getNombre()).thenReturn("WS-1");
        when(computadoraRepository.findById(5L)).thenReturn(Optional.of(c));
        when(sesionRepository.findFirstByComputadora_IdAndEstadoAndFinEnIsNull(5L, EstadoSesion.ACTIVA))
                .thenReturn(Optional.empty());
        when(visibilidadService.puedeVer(10L, 5L)).thenReturn(true);
        when(usuarioRepository.findById(10L)).thenReturn(Optional.of(mock(Usuario.class)));

        EmisionLanzamiento r = servicio().emitir(10L, "e@x.com", Rol.EMPLEADO, 5L,
                TipoLanzamiento.CONECTAR, "100.0.0.9");

        assertThat(r.exito()).isTrue();
        assertThat(r.token()).isNotBlank();
        assertThat(r.urlProtocolo()).startsWith("pulsedesk://redimir?token=");
        verify(lanzamientoRepository).save(any(Lanzamiento.class));
        verify(auditoriaService).lanzamientoRdp(eq(10L), eq("e@x.com"), eq(5L), eq("100.0.0.9"), anyString());
    }

    @Test
    void empleadoConectaPcOcupada_rechazaYaudita() {
        Computadora c = pcActivaConCredencial();
        when(computadoraRepository.findById(5L)).thenReturn(Optional.of(c));
        when(sesionRepository.findFirstByComputadora_IdAndEstadoAndFinEnIsNull(5L, EstadoSesion.ACTIVA))
                .thenReturn(Optional.of(mock(SesionRdp.class)));
        when(visibilidadService.puedeVer(10L, 5L)).thenReturn(true);

        EmisionLanzamiento r = servicio().emitir(10L, "e@x.com", Rol.EMPLEADO, 5L,
                TipoLanzamiento.CONECTAR, "ip");

        assertThat(r.exito()).isFalse();
        assertThat(r.mensaje()).contains("ocupada");
        verify(lanzamientoRepository, never()).save(any());
        verify(auditoriaService).lanzamientoRdp(eq(10L), anyString(), eq(5L), anyString(), contains("RECHAZADO"));
    }

    @Test
    void empleadoSinVisibilidad_rechaza() {
        Computadora c = pcActivaConCredencial();
        when(computadoraRepository.findById(5L)).thenReturn(Optional.of(c));
        when(sesionRepository.findFirstByComputadora_IdAndEstadoAndFinEnIsNull(5L, EstadoSesion.ACTIVA))
                .thenReturn(Optional.empty());
        when(visibilidadService.puedeVer(10L, 5L)).thenReturn(false);

        EmisionLanzamiento r = servicio().emitir(10L, "e@x.com", Rol.EMPLEADO, 5L,
                TipoLanzamiento.CONECTAR, "ip");

        assertThat(r.exito()).isFalse();
        verify(lanzamientoRepository, never()).save(any());
    }

    @Test
    void empleadoNoPuedeShadow() {
        Computadora c = pcActivaConCredencial();
        when(computadoraRepository.findById(5L)).thenReturn(Optional.of(c));
        when(sesionRepository.findFirstByComputadora_IdAndEstadoAndFinEnIsNull(5L, EstadoSesion.ACTIVA))
                .thenReturn(Optional.of(mock(SesionRdp.class)));

        EmisionLanzamiento r = servicio().emitir(10L, "e@x.com", Rol.EMPLEADO, 5L,
                TipoLanzamiento.SHADOW, "ip");

        assertThat(r.exito()).isFalse();
        verify(auditoriaService).shadow(eq(10L), anyString(), eq(5L), contains("RECHAZADO"));
    }

    @Test
    void managerShadowPcOcupada_emiteConSesionObjetivo() {
        Computadora c = pcActivaConCredencial();
        when(c.getNombre()).thenReturn("WS-9");
        SesionRdp s = mock(SesionRdp.class);
        when(s.getIdSesionWindows()).thenReturn("3");
        when(computadoraRepository.findById(5L)).thenReturn(Optional.of(c));
        when(sesionRepository.findFirstByComputadora_IdAndEstadoAndFinEnIsNull(5L, EstadoSesion.ACTIVA))
                .thenReturn(Optional.of(s));
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(mock(Usuario.class)));

        EmisionLanzamiento r = servicio().emitir(2L, "m@x.com", Rol.MANAGER, 5L,
                TipoLanzamiento.SHADOW, "ip");

        assertThat(r.exito()).isTrue();
        assertThat(r.tipo()).isEqualTo(TipoLanzamiento.SHADOW);
        verify(auditoriaService).shadow(eq(2L), anyString(), eq(5L), contains("sesión 3"));
    }

    @Test
    void managerConectaPcOcupada_auditaExpulsion() {
        Computadora c = pcActivaConCredencial();
        when(c.getCredencialCifrada()).thenReturn(new byte[] {1});
        when(c.getNombre()).thenReturn("WS-2");
        SesionRdp s = mock(SesionRdp.class);
        when(s.getUsuarioWindows()).thenReturn("DOM\\juan");
        when(computadoraRepository.findById(5L)).thenReturn(Optional.of(c));
        when(sesionRepository.findFirstByComputadora_IdAndEstadoAndFinEnIsNull(5L, EstadoSesion.ACTIVA))
                .thenReturn(Optional.of(s));
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(mock(Usuario.class)));

        EmisionLanzamiento r = servicio().emitir(2L, "m@x.com", Rol.MANAGER, 5L,
                TipoLanzamiento.CONECTAR, "ip");

        assertThat(r.exito()).isTrue();
        verify(auditoriaService).expulsion(eq(2L), eq("m@x.com"), eq(5L), contains("juan"));
    }

    // ---------- canjear ----------

    private Lanzamiento lanzamiento(TipoLanzamiento tipo, Instant vence, String idSesion) {
        // Computadora real (sin stubbing) para que los canjes rechazados temprano no dejen stubs sin usar.
        Computadora c = new Computadora("WS-1", null, "100.1.1.1");
        c.setCuentaUsuario("op");
        if (tipo == TipoLanzamiento.CONECTAR) {
            c.setCredencialCifrada(new byte[] {9});
        }
        Usuario u = mock(Usuario.class);
        lenient().when(u.getId()).thenReturn(10L);
        return new Lanzamiento(u, c, tipo, "hash", AHORA.minusSeconds(10), vence, idSesion);
    }

    private PreferenciasVista prefsPorDefecto() {
        return new PreferenciasVista(true, false, false, 100, SonidoRdp.LOCAL, false,
                AtajosRdp.SOLO_PANTALLA_COMPLETA, CalidadRdp.EQUILIBRADA);
    }

    @Test
    void canjearConectar_devuelvePasswordYmarcaCanjeado() {
        Lanzamiento l = lanzamiento(TipoLanzamiento.CONECTAR, AHORA.plusSeconds(60), null);
        when(lanzamientoRepository.findByTokenHash(anyString())).thenReturn(Optional.of(l));
        when(preferenciasService.obtener(10L)).thenReturn(prefsPorDefecto());
        when(cifrador.descifrar(any())).thenReturn("secreta");

        CanjeResultado r = servicio().canjear("eltoken", "100.9.9.9");

        assertThat(r.tipo()).isEqualTo("CONECTAR");
        assertThat(r.password()).isEqualTo("secreta");
        assertThat(r.host()).isEqualTo("100.1.1.1");
        assertThat(l.getEstado()).isEqualTo(EstadoLanzamiento.CANJEADO);
        assertThat(l.getIpHelper()).isEqualTo("100.9.9.9");
    }

    @Test
    void canjearShadow_sinPasswordConSesion() {
        Lanzamiento l = lanzamiento(TipoLanzamiento.SHADOW, AHORA.plusSeconds(60), "7");
        when(lanzamientoRepository.findByTokenHash(anyString())).thenReturn(Optional.of(l));
        when(preferenciasService.obtener(10L)).thenReturn(prefsPorDefecto());

        CanjeResultado r = servicio().canjear("eltoken", "ip");

        assertThat(r.tipo()).isEqualTo("SHADOW");
        assertThat(r.password()).isNull();
        assertThat(r.idSesionObjetivo()).isEqualTo("7");
    }

    @Test
    void canjearDosVeces_rechazaElSegundo() {
        Lanzamiento l = lanzamiento(TipoLanzamiento.CONECTAR, AHORA.plusSeconds(60), null);
        l.setEstado(EstadoLanzamiento.CANJEADO);
        when(lanzamientoRepository.findByTokenHash(anyString())).thenReturn(Optional.of(l));

        assertThatThrownBy(() -> servicio().canjear("eltoken", "ip"))
                .isInstanceOf(ReglaNegocioException.class);
    }

    @Test
    void canjearVencido_rechaza() {
        Lanzamiento l = lanzamiento(TipoLanzamiento.CONECTAR, AHORA.minusSeconds(1), null);
        when(lanzamientoRepository.findByTokenHash(anyString())).thenReturn(Optional.of(l));

        assertThatThrownBy(() -> servicio().canjear("eltoken", "ip"))
                .isInstanceOf(ReglaNegocioException.class);
        assertThat(l.getEstado()).isEqualTo(EstadoLanzamiento.EXPIRADO);
    }

    @Test
    void canjearTokenDesconocido_rechaza() {
        when(lanzamientoRepository.findByTokenHash(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio().canjear("x", "ip"))
                .isInstanceOf(ReglaNegocioException.class);
    }
}
