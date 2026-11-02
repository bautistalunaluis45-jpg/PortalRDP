package com.agentesocial.portalrdp.servicio;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.agentesocial.portalrdp.dominio.AccesoTemporal;
import com.agentesocial.portalrdp.dominio.EstadoSesion;
import com.agentesocial.portalrdp.dominio.SesionRdp;
import com.agentesocial.portalrdp.infraestructura.PropiedadesEstado;
import com.agentesocial.portalrdp.repositorio.AccesoTemporalRepository;
import com.agentesocial.portalrdp.repositorio.SesionRdpRepository;
import com.agentesocial.portalrdp.repositorio.UsuarioRepository;

/**
 * Arma el panel "Resumen en vivo" (manager/admin) con datos reales del momento:
 * KPIs de computadoras, quién está conectado y quién está idle, PCs que requieren
 * atención y accesos temporales por vencer. No inventa: si no hay datos, van listas vacías.
 */
@Service
public class ResumenService {

    private final ComputadoraService computadoraService;
    private final SesionRdpRepository sesionRepository;
    private final AccesoTemporalRepository accesoRepository;
    private final PropiedadesEstado propiedadesEstado;
    private final Clock reloj;
    private final SesionActivaConsulta sesiones;

    public ResumenService(ComputadoraService computadoraService,
                          SesionRdpRepository sesionRepository,
                          AccesoTemporalRepository accesoRepository,
                          PropiedadesEstado propiedadesEstado,
                          Clock reloj,
                          UsuarioRepository usuarioRepository) {
        this.computadoraService = computadoraService;
        this.sesionRepository = sesionRepository;
        this.accesoRepository = accesoRepository;
        this.propiedadesEstado = propiedadesEstado;
        this.reloj = reloj;
        this.sesiones = new SesionActivaConsulta(sesionRepository, usuarioRepository);
    }

    @Transactional(readOnly = true)
    public ResumenVista enVivo() {
        Instant ahora = reloj.instant();
        long umbralIdle = propiedadesEstado.getIdleTrasSegundos();

        ComputadorasResumen r = computadoraService.resumen();

        // Sesiones conectadas ahora.
        List<ResumenVista.SesionEnVivo> enUso = new ArrayList<>();
        int idleCount = 0;
        for (SesionRdp s : sesionRepository.findByEstadoAndFinEnIsNullOrderByDesdeAsc(EstadoSesion.ACTIVA)) {
            boolean idle = s.getIdleSegundos() >= umbralIdle;
            if (idle) {
                idleCount++;
            }
            enUso.add(new ResumenVista.SesionEnVivo(
                    s.getComputadora().getNombre(),
                    s.getComputadora().getDepartamento() != null ? s.getComputadora().getDepartamento().getNombre() : "—",
                    sesiones.nombreMostrado(s),
                    "hace " + duracion(Duration.between(s.getDesde(), ahora)),
                    idle ? duracion(Duration.ofSeconds(s.getIdleSegundos())) + " sin actividad" : "activo",
                    idle));
        }

        // PCs que requieren atención (reusa la lista de admin).
        List<ResumenVista.PcAtencion> sinConexion = new ArrayList<>();
        List<ResumenVista.PcAtencion> sinCredencial = new ArrayList<>();
        for (ComputadoraAdminVista c : computadoraService.listar()) {
            if (!c.activa()) {
                continue;
            }
            if (c.esSinConexion()) {
                sinConexion.add(new ResumenVista.PcAtencion(c.nombre(), c.departamentoNombre(),
                        "Último reporte: " + c.ultimoLatidoTexto()));
            }
            if (!c.credencialConfigurada()) {
                sinCredencial.add(new ResumenVista.PcAtencion(c.nombre(), c.departamentoNombre(),
                        "Sin contraseña configurada"));
            }
        }

        // Accesos temporales vigentes y los que vencen dentro de 24 h.
        List<AccesoTemporal> vigentes = accesoRepository.vigentes(ahora);
        Instant limite = ahora.plus(Duration.ofHours(24));
        List<ResumenVista.AccesoPorVencer> porVencer = new ArrayList<>();
        for (AccesoTemporal a : vigentes) {
            if (a.getVenceEn() != null && !a.getVenceEn().isAfter(limite)) {
                porVencer.add(new ResumenVista.AccesoPorVencer(
                        a.getUsuario().getNombre(), a.getComputadora().getNombre(),
                        "vence en " + duracion(Duration.between(ahora, a.getVenceEn()))));
            }
        }

        return new ResumenVista(
                (int) r.total(), (int) r.libres(), (int) r.ocupadas(), (int) r.sinConexion(),
                enUso, idleCount, sinConexion, sinCredencial, vigentes.size(), porVencer);
    }

    /** Formatea una duración como "Xh Ym" o "Nm" (o "<1m"). */
    private static String duracion(Duration d) {
        long min = Math.max(0, d.toMinutes());
        if (min < 1) {
            return "<1m";
        }
        if (min < 60) {
            return min + "m";
        }
        return (min / 60) + "h " + (min % 60) + "m";
    }
}
