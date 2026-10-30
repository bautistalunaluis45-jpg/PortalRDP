package com.agentesocial.portalrdp.servicio;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.agentesocial.portalrdp.dominio.AccesoTemporal;
import com.agentesocial.portalrdp.dominio.Computadora;
import com.agentesocial.portalrdp.dominio.EstadoComputadora;
import com.agentesocial.portalrdp.dominio.Usuario;
import com.agentesocial.portalrdp.infraestructura.PropiedadesEstado;
import com.agentesocial.portalrdp.repositorio.AccesoTemporalRepository;
import com.agentesocial.portalrdp.repositorio.ComputadoraRepository;
import com.agentesocial.portalrdp.repositorio.SesionRdpRepository;
import com.agentesocial.portalrdp.repositorio.UsuarioRepository;

/**
 * Decide que computadoras ve un usuario (caso E1) y calcula su estado.
 * El aislamiento por departamento se aplica aqui (en el servidor), no solo en la vista.
 * Ver docs/decisiones.md secciones 3, 6 y 9, y docs/casos-de-uso.md E1.
 */
@Service
public class VisibilidadService {

    private static final DateTimeFormatter FORMATO_VIGENCIA =
            DateTimeFormatter.ofPattern("d MMM, HH:mm", new Locale("es"));

    private final UsuarioRepository usuarioRepository;
    private final ComputadoraRepository computadoraRepository;
    private final AccesoTemporalRepository accesoTemporalRepository;
    private final SesionActivaConsulta sesiones;
    private final Clock reloj;
    private final PropiedadesEstado propiedadesEstado;

    public VisibilidadService(UsuarioRepository usuarioRepository,
                              ComputadoraRepository computadoraRepository,
                              AccesoTemporalRepository accesoTemporalRepository,
                              SesionRdpRepository sesionRdpRepository,
                              Clock reloj,
                              PropiedadesEstado propiedadesEstado) {
        this.usuarioRepository = usuarioRepository;
        this.computadoraRepository = computadoraRepository;
        this.accesoTemporalRepository = accesoTemporalRepository;
        this.sesiones = new SesionActivaConsulta(sesionRdpRepository);
        this.reloj = reloj;
        this.propiedadesEstado = propiedadesEstado;
    }

    @Transactional(readOnly = true)
    public MisComputadorasVista misComputadoras(Long usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario inexistente: " + usuarioId));

        Instant ahora = reloj.instant();
        Duration umbral = propiedadesEstado.umbralSinConexion();
        ZoneId zona = reloj.getZone();

        List<Computadora> pcsDepartamento = computadoraRepository
                .findByDepartamento_IdAndActivaTrueOrderByNombreAsc(usuario.getDepartamento().getId());

        List<AccesoTemporal> accesos = accesoTemporalRepository.vigentesDeUsuario(usuarioId, ahora);

        // Sesiones conectadas de todas las PCs relevantes (para OCUPADA / "en uso por").
        List<Long> ids = new ArrayList<>();
        pcsDepartamento.forEach(c -> ids.add(c.getId()));
        accesos.forEach(a -> ids.add(a.getComputadora().getId()));
        Map<Long, com.agentesocial.portalrdp.dominio.SesionRdp> activas = sesiones.mapaActivas(ids);

        List<ComputadoraVista> departamento = new ArrayList<>();
        for (Computadora c : pcsDepartamento) {
            departamento.add(aVista(c, ahora, umbral, false, null, activas.get(c.getId())));
        }

        List<ComputadoraVista> temporales = new ArrayList<>();
        for (AccesoTemporal acceso : accesos) {
            Computadora c = acceso.getComputadora();
            if (!c.isActiva()) {
                continue;
            }
            // Evita duplicar si la PC ya aparece por departamento.
            boolean yaEnDepartamento = pcsDepartamento.stream()
                    .anyMatch(d -> d.getId() != null && d.getId().equals(c.getId()));
            if (yaEnDepartamento) {
                continue;
            }
            String vigencia = acceso.getVenceEn() == null
                    ? "Acceso indefinido"
                    : "Vence " + FORMATO_VIGENCIA.format(acceso.getVenceEn().atZone(zona));
            temporales.add(aVista(c, ahora, umbral, true, vigencia, activas.get(c.getId())));
        }

        List<ComputadoraVista> todas = new ArrayList<>(departamento);
        todas.addAll(temporales);
        int total = todas.size();
        int libres = (int) todas.stream().filter(v -> v.estado() == EstadoComputadora.LIBRE).count();
        int ocupadas = (int) todas.stream().filter(v -> v.estado() == EstadoComputadora.OCUPADA).count();
        int sinConexion = (int) todas.stream().filter(v -> v.estado() == EstadoComputadora.SIN_CONEXION).count();

        return new MisComputadorasVista(
                usuario.getDepartamento().getNombre(),
                departamento, temporales, total, libres, ocupadas, sinConexion);
    }

    private ComputadoraVista aVista(Computadora c, Instant ahora, Duration umbral, boolean temporal,
                                    String vigencia, com.agentesocial.portalrdp.dominio.SesionRdp activa) {
        boolean ocupada = activa != null;
        EstadoComputadora estado = EstadoCalculo.calcular(c.getUltimoLatido(), ahora, umbral, ocupada);
        String usuarioEnUso = estado == EstadoComputadora.OCUPADA ? SesionActivaConsulta.nombreEnUso(activa) : null;
        return new ComputadoraVista(c.getId(), c.getNombre(), estado, usuarioEnUso, temporal, vigencia);
    }
}
