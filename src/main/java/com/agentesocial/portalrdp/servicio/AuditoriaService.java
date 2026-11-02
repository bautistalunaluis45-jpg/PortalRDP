package com.agentesocial.portalrdp.servicio;

import java.time.Clock;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.agentesocial.portalrdp.dominio.EventoAuditoria;
import com.agentesocial.portalrdp.dominio.TipoEventoAuditoria;
import com.agentesocial.portalrdp.repositorio.EventoAuditoriaRepository;

/**
 * Registra eventos de auditoria. Por diseno NO llama a otros servicios (regla 4);
 * los demas servicios lo llaman dentro de su propia transaccion para que la
 * auditoria quede en la misma transaccion que la accion (docs/decisiones.md 17).
 */
@Service
public class AuditoriaService {

    private static final DateTimeFormatter FORMATO_FECHA =
            DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm", new Locale("es"));

    private final EventoAuditoriaRepository repositorio;
    private final Clock reloj;

    public AuditoriaService(EventoAuditoriaRepository repositorio, Clock reloj) {
        this.repositorio = repositorio;
        this.reloj = reloj;
    }

    /** Fecha del último acceso al portal del usuario (última ENTRADA_PORTAL), o "Sin registro". */
    @Transactional(readOnly = true)
    public String ultimoAcceso(Long usuarioId) {
        return repositorio
                .findFirstByActorUsuarioIdAndTipoOrderByOcurridoEnDesc(usuarioId, TipoEventoAuditoria.ENTRADA_PORTAL)
                .map(e -> FORMATO_FECHA.format(e.getOcurridoEn().atZone(reloj.getZone())))
                .orElse("Sin registro");
    }

    /** Últimos 5 eventos del usuario (actor o afectado) para el panel del detalle. */
    @Transactional(readOnly = true)
    public List<EventoAuditoriaVista> ultimosDeUsuario(Long usuarioId) {
        return aVistas(repositorio
                .findTop5ByActorUsuarioIdOrUsuarioAfectadoIdOrderByOcurridoEnDesc(usuarioId, usuarioId));
    }

    /** Historial completo del usuario (actor o afectado) para su pantalla de auditoría. */
    @Transactional(readOnly = true)
    public List<EventoAuditoriaVista> historialDeUsuario(Long usuarioId) {
        return aVistas(repositorio
                .findByActorUsuarioIdOrUsuarioAfectadoIdOrderByOcurridoEnDesc(usuarioId, usuarioId));
    }

    private List<EventoAuditoriaVista> aVistas(List<EventoAuditoria> eventos) {
        return eventos.stream()
                .map(e -> new EventoAuditoriaVista(
                        e.getTipo().etiqueta(),
                        e.getDetalle(),
                        FORMATO_FECHA.format(e.getOcurridoEn().atZone(reloj.getZone())),
                        e.getActorCorreo()))
                .toList();
    }

    /** Punto de registro general. Participa en la transaccion del que lo invoca. */
    @Transactional
    public void registrar(EventoAuditoria evento) {
        repositorio.save(evento);
    }

    @Transactional
    public void entradaPortal(Long actorId, String actorCorreo, String ipOrigen) {
        registrar(EventoAuditoria.de(TipoEventoAuditoria.ENTRADA_PORTAL, reloj.instant())
                .actor(actorId, actorCorreo)
                .ipOrigen(ipOrigen)
                .construir());
    }

    @Transactional
    public void salidaPortal(Long actorId, String actorCorreo, String ipOrigen) {
        registrar(EventoAuditoria.de(TipoEventoAuditoria.SALIDA_PORTAL, reloj.instant())
                .actor(actorId, actorCorreo)
                .ipOrigen(ipOrigen)
                .construir());
    }

    @Transactional
    public void cambioDepartamento(Long actorId, String actorCorreo, String departamentoNombre, String detalle) {
        registrar(EventoAuditoria.de(TipoEventoAuditoria.CAMBIO_DEPARTAMENTO, reloj.instant())
                .actor(actorId, actorCorreo)
                .departamento(departamentoNombre)
                .detalle(detalle)
                .construir());
    }

    @Transactional
    public void cambioUsuario(Long actorId, String actorCorreo, Long usuarioAfectadoId, String detalle) {
        registrar(EventoAuditoria.de(TipoEventoAuditoria.CAMBIO_USUARIO, reloj.instant())
                .actor(actorId, actorCorreo)
                .usuarioAfectado(usuarioAfectadoId)
                .detalle(detalle)
                .construir());
    }

    /** Lanzamiento de RDP (incluye rechazos y fallos, por eso lleva el resultado en el detalle). */
    @Transactional
    public void lanzamientoRdp(Long actorId, String actorCorreo, Long computadoraId, String ipOrigen, String detalle) {
        registrar(EventoAuditoria.de(TipoEventoAuditoria.LANZAMIENTO_RDP, reloj.instant())
                .actor(actorId, actorCorreo)
                .computadora(computadoraId)
                .ipOrigen(ipOrigen)
                .detalle(detalle)
                .construir());
    }

    /** Supervisión (shadow) de una sesión. */
    @Transactional
    public void shadow(Long actorId, String actorCorreo, Long computadoraId, String detalle) {
        registrar(EventoAuditoria.de(TipoEventoAuditoria.SHADOW, reloj.instant())
                .actor(actorId, actorCorreo)
                .computadora(computadoraId)
                .detalle(detalle)
                .construir());
    }

    /** Expulsión: un manager/admin se conecta a una PC ocupada y saca al usuario anterior. */
    @Transactional
    public void expulsion(Long actorId, String actorCorreo, Long computadoraId, String detalle) {
        registrar(EventoAuditoria.de(TipoEventoAuditoria.EXPULSION, reloj.instant())
                .actor(actorId, actorCorreo)
                .computadora(computadoraId)
                .detalle(detalle)
                .construir());
    }

    /** Orden de desconectar a un usuario (sin cerrar su sesión). */
    @Transactional
    public void desconectar(Long actorId, String actorCorreo, Long computadoraId, String detalle) {
        registrar(EventoAuditoria.de(TipoEventoAuditoria.DESCONECTAR, reloj.instant())
                .actor(actorId, actorCorreo)
                .computadora(computadoraId)
                .detalle(detalle)
                .construir());
    }

    /** Fin de una sesión reportado por el agente (evento del sistema, sin actor humano). */
    @Transactional
    public void finSesion(Long computadoraId, String detalle) {
        registrar(EventoAuditoria.de(TipoEventoAuditoria.FIN_SESION, reloj.instant())
                .computadora(computadoraId)
                .detalle(detalle)
                .construir());
    }

    @Transactional
    public void cambioComputadora(Long actorId, String actorCorreo, Long computadoraId, String detalle) {
        registrar(EventoAuditoria.de(TipoEventoAuditoria.CAMBIO_COMPUTADORA, reloj.instant())
                .actor(actorId, actorCorreo)
                .computadora(computadoraId)
                .detalle(detalle)
                .construir());
    }

    @Transactional
    public void accesoTemporalCreado(Long actorId, String actorCorreo, Long usuarioAfectadoId,
                                     Long computadoraId, String detalle) {
        registrar(EventoAuditoria.de(TipoEventoAuditoria.ACCESO_TEMPORAL_CREADO, reloj.instant())
                .actor(actorId, actorCorreo)
                .usuarioAfectado(usuarioAfectadoId)
                .computadora(computadoraId)
                .detalle(detalle)
                .construir());
    }

    @Transactional
    public void accesoTemporalRevocado(Long actorId, String actorCorreo, Long usuarioAfectadoId,
                                       Long computadoraId, String detalle) {
        registrar(EventoAuditoria.de(TipoEventoAuditoria.ACCESO_TEMPORAL_REVOCADO, reloj.instant())
                .actor(actorId, actorCorreo)
                .usuarioAfectado(usuarioAfectadoId)
                .computadora(computadoraId)
                .detalle(detalle)
                .construir());
    }

    /** Eliminación DEFINITIVA de un usuario (el log se conserva aunque el usuario ya no exista). */
    @Transactional
    public void usuarioEliminado(Long actorId, String actorCorreo, Long usuarioAfectadoId, String detalle) {
        registrar(EventoAuditoria.de(TipoEventoAuditoria.USUARIO_ELIMINADO, reloj.instant())
                .actor(actorId, actorCorreo)
                .usuarioAfectado(usuarioAfectadoId)
                .detalle(detalle)
                .construir());
    }

    /** Eliminación DEFINITIVA de una computadora (el log se conserva aunque la PC ya no exista). */
    @Transactional
    public void computadoraEliminada(Long actorId, String actorCorreo, Long computadoraId, String detalle) {
        registrar(EventoAuditoria.de(TipoEventoAuditoria.COMPUTADORA_ELIMINADA, reloj.instant())
                .actor(actorId, actorCorreo)
                .computadora(computadoraId)
                .detalle(detalle)
                .construir());
    }

    /** Cambio de la contraseña de una PC. Nunca se guarda la contraseña, solo el hecho. */
    @Transactional
    public void cambioPasswordPc(Long actorId, String actorCorreo, Long computadoraId) {
        registrar(EventoAuditoria.de(TipoEventoAuditoria.CAMBIO_PASSWORD_PC, reloj.instant())
                .actor(actorId, actorCorreo)
                .computadora(computadoraId)
                .detalle("Se actualizó la contraseña de la cuenta de la PC")
                .construir());
    }
}
