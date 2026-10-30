package com.agentesocial.portalrdp.servicio;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.agentesocial.portalrdp.dominio.AccesoTemporal;
import com.agentesocial.portalrdp.dominio.Computadora;
import com.agentesocial.portalrdp.dominio.Usuario;
import com.agentesocial.portalrdp.excepcion.ReglaNegocioException;
import com.agentesocial.portalrdp.repositorio.AccesoTemporalRepository;
import com.agentesocial.portalrdp.repositorio.ComputadoraRepository;
import com.agentesocial.portalrdp.repositorio.UsuarioRepository;

/** Otorgar, listar y revocar accesos temporales (A8, A9). Registra auditoría. */
@Service
public class AccesoTemporalService {

    private static final DateTimeFormatter FORMATO = DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm", new Locale("es"));

    private final AccesoTemporalRepository accesoTemporalRepository;
    private final UsuarioRepository usuarioRepository;
    private final ComputadoraRepository computadoraRepository;
    private final Clock reloj;
    private final AuditoriaService auditoriaService;

    public AccesoTemporalService(AccesoTemporalRepository accesoTemporalRepository,
                                 UsuarioRepository usuarioRepository,
                                 ComputadoraRepository computadoraRepository,
                                 Clock reloj,
                                 AuditoriaService auditoriaService) {
        this.accesoTemporalRepository = accesoTemporalRepository;
        this.usuarioRepository = usuarioRepository;
        this.computadoraRepository = computadoraRepository;
        this.reloj = reloj;
        this.auditoriaService = auditoriaService;
    }

    @Transactional(readOnly = true)
    public List<AccesoTemporalVista> listarVigentes() {
        Instant ahora = reloj.instant();
        List<AccesoTemporalVista> vistas = new ArrayList<>();
        for (AccesoTemporal a : accesoTemporalRepository.vigentes(ahora)) {
            Usuario u = a.getUsuario();
            Computadora c = a.getComputadora();
            String venceTexto = a.getVenceEn() == null ? "Indefinido"
                    : FORMATO.format(a.getVenceEn().atZone(reloj.getZone()));
            vistas.add(new AccesoTemporalVista(
                    a.getId(), u.getNombre(), u.getCorreo(), iniciales(u.getNombre()),
                    c.getNombre(), c.getDepartamento().getNombre(),
                    venceTexto, restante(ahora, a.getVenceEn())));
        }
        return vistas;
    }

    /** Otorga un acceso temporal. {@code horas} nulo = indefinido (sin vencimiento). */
    @Transactional
    public void otorgar(Long usuarioId, Long computadoraId, Integer horas, String motivo,
                        Long actorId, String actorCorreo) {
        if (usuarioId == null) {
            throw new ReglaNegocioException("Selecciona un usuario.");
        }
        if (computadoraId == null) {
            throw new ReglaNegocioException("Selecciona una computadora.");
        }
        if (horas != null && horas <= 0) {
            throw new ReglaNegocioException("La duración debe ser mayor a cero.");
        }
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .filter(Usuario::isActivo)
                .orElseThrow(() -> new ReglaNegocioException("El usuario no existe o está inactivo."));
        Computadora computadora = computadoraRepository.findById(computadoraId)
                .filter(Computadora::isActiva)
                .orElseThrow(() -> new ReglaNegocioException("La computadora no existe o está inactiva."));

        Instant ahora = reloj.instant();
        if (accesoTemporalRepository.existeVigente(usuarioId, computadoraId, ahora)) {
            throw new ReglaNegocioException("Ese usuario ya tiene un acceso vigente a esa computadora.");
        }
        Usuario otorgante = usuarioRepository.findById(actorId).orElse(null);
        Instant venceEn = horas == null ? null : ahora.plus(Duration.ofHours(horas));
        String motivoLimpio = StringUtils.hasText(motivo) ? motivo.trim() : null;

        AccesoTemporal acceso = new AccesoTemporal(usuario, computadora, venceEn, otorgante, motivoLimpio);
        accesoTemporalRepository.save(acceso);
        String vigencia = horas == null ? "indefinido" : ("por " + horas + " h");
        auditoriaService.accesoTemporalCreado(actorId, actorCorreo, usuarioId, computadoraId,
                "Acceso temporal a " + computadora.getNombre() + " para " + usuario.getCorreo()
                        + " " + vigencia + (motivoLimpio != null ? " (" + motivoLimpio + ")" : ""));
    }

    @Transactional
    public void revocar(Long id, Long actorId, String actorCorreo) {
        AccesoTemporal acceso = accesoTemporalRepository.findById(id)
                .orElseThrow(() -> new ReglaNegocioException("El acceso temporal no existe."));
        if (acceso.getRevocadoEn() == null) {
            acceso.setRevocadoEn(reloj.instant());
        }
        auditoriaService.accesoTemporalRevocado(actorId, actorCorreo,
                acceso.getUsuario().getId(), acceso.getComputadora().getId(),
                "Revocación de acceso a " + acceso.getComputadora().getNombre()
                        + " para " + acceso.getUsuario().getCorreo());
    }

    private static String iniciales(String nombre) {
        if (!StringUtils.hasText(nombre)) {
            return "?";
        }
        String[] p = nombre.trim().split("\\s+");
        String s = p[0].substring(0, 1);
        if (p.length > 1) {
            s += p[p.length - 1].substring(0, 1);
        }
        return s.toUpperCase();
    }

    private static String restante(Instant ahora, Instant venceEn) {
        if (venceEn == null) {
            return "Hasta revocar";
        }
        Duration d = Duration.between(ahora, venceEn);
        if (d.isNegative() || d.isZero()) {
            return "Vencido";
        }
        long dias = d.toDays();
        if (dias >= 1) {
            return "Quedan " + dias + (dias == 1 ? " día" : " días");
        }
        long horas = d.toHours();
        long minutos = d.toMinutesPart();
        if (horas >= 1) {
            return "Quedan " + horas + "h " + minutos + "m";
        }
        return "Quedan " + d.toMinutes() + "m";
    }
}
