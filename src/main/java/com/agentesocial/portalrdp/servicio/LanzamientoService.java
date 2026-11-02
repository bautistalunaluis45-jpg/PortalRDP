package com.agentesocial.portalrdp.servicio;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.agentesocial.portalrdp.dominio.Computadora;
import com.agentesocial.portalrdp.dominio.EstadoSesion;
import com.agentesocial.portalrdp.dominio.Lanzamiento;
import com.agentesocial.portalrdp.dominio.EstadoLanzamiento;
import com.agentesocial.portalrdp.dominio.Rol;
import com.agentesocial.portalrdp.dominio.SesionRdp;
import com.agentesocial.portalrdp.dominio.TipoLanzamiento;
import com.agentesocial.portalrdp.dominio.Usuario;
import com.agentesocial.portalrdp.excepcion.ReglaNegocioException;
import com.agentesocial.portalrdp.infraestructura.Cifrador;
import com.agentesocial.portalrdp.infraestructura.PropiedadesLanzamiento;
import com.agentesocial.portalrdp.infraestructura.Tokens;
import com.agentesocial.portalrdp.repositorio.ComputadoraRepository;
import com.agentesocial.portalrdp.repositorio.LanzamientoRepository;
import com.agentesocial.portalrdp.repositorio.SesionRdpRepository;
import com.agentesocial.portalrdp.repositorio.UsuarioRepository;

/**
 * Emite y canjea tokens de lanzamiento de un solo uso (fase 5, docs/decisiones.md 5).
 * Valida rol/visibilidad/estado, audita (incluidos los rechazos) y entrega al helper
 * host, cuenta, contraseña (solo aquí sale del portal) y preferencias.
 *
 * Los rechazos de negocio NO lanzan excepción: devuelven un resultado para que la
 * auditoría del rechazo quede persistida (una excepción haría rollback y la borraría).
 */
@Service
public class LanzamientoService {

    private final LanzamientoRepository lanzamientoRepository;
    private final ComputadoraRepository computadoraRepository;
    private final UsuarioRepository usuarioRepository;
    private final SesionRdpRepository sesionRepository;
    private final VisibilidadService visibilidadService;
    private final AuditoriaService auditoriaService;
    private final PreferenciasService preferenciasService;
    private final Cifrador cifrador;
    private final Clock reloj;
    private final PropiedadesLanzamiento propiedades;

    public LanzamientoService(LanzamientoRepository lanzamientoRepository,
                              ComputadoraRepository computadoraRepository,
                              UsuarioRepository usuarioRepository,
                              SesionRdpRepository sesionRepository,
                              VisibilidadService visibilidadService,
                              AuditoriaService auditoriaService,
                              PreferenciasService preferenciasService,
                              Cifrador cifrador,
                              Clock reloj,
                              PropiedadesLanzamiento propiedades) {
        this.lanzamientoRepository = lanzamientoRepository;
        this.computadoraRepository = computadoraRepository;
        this.usuarioRepository = usuarioRepository;
        this.sesionRepository = sesionRepository;
        this.visibilidadService = visibilidadService;
        this.auditoriaService = auditoriaService;
        this.preferenciasService = preferenciasService;
        this.cifrador = cifrador;
        this.reloj = reloj;
        this.propiedades = propiedades;
    }

    @Transactional
    public EmisionLanzamiento emitir(Long actorId, String actorCorreo, Rol actorRol,
                                     Long computadoraId, TipoLanzamiento tipo, String ipOrigen) {
        Computadora c = computadoraRepository.findById(computadoraId).orElse(null);
        if (c == null) {
            auditarRechazo(tipo, actorId, actorCorreo, computadoraId, ipOrigen, "la computadora no existe");
            return EmisionLanzamiento.rechazo("La computadora no existe.");
        }
        if (!c.isActiva()) {
            auditarRechazo(tipo, actorId, actorCorreo, computadoraId, ipOrigen, "la computadora está dada de baja");
            return EmisionLanzamiento.rechazo("La computadora está dada de baja.");
        }

        Optional<SesionRdp> activa = sesionRepository
                .findFirstByComputadora_IdAndEstadoAndFinEnIsNull(computadoraId, EstadoSesion.ACTIVA);
        boolean ocupada = activa.isPresent();
        String idSesionObjetivo = null;

        if (tipo == TipoLanzamiento.CONECTAR) {
            if (actorRol == Rol.EMPLEADO) {
                if (!visibilidadService.puedeVer(actorId, computadoraId)) {
                    auditarRechazo(tipo, actorId, actorCorreo, computadoraId, ipOrigen, "sin visibilidad sobre la PC");
                    return EmisionLanzamiento.rechazo("No tienes acceso a esta computadora.");
                }
                if (ocupada) {
                    auditarRechazo(tipo, actorId, actorCorreo, computadoraId, ipOrigen, "la PC está ocupada");
                    return EmisionLanzamiento.rechazo("La computadora está ocupada en este momento.");
                }
            } else if (ocupada) {
                // Manager/admin pueden tomar una PC ocupada: eso expulsa al usuario anterior.
                auditoriaService.expulsion(actorId, actorCorreo, computadoraId,
                        "Conexión sobre la sesión en uso de " + SesionActivaConsulta.nombreEnUso(activa.get())
                                + " en " + c.getNombre());
            }
            if (c.getCredencialCifrada() == null) {
                auditarRechazo(tipo, actorId, actorCorreo, computadoraId, ipOrigen, "la PC no tiene contraseña configurada");
                return EmisionLanzamiento.rechazo("La PC no tiene contraseña configurada; no se puede lanzar.");
            }
        } else { // SHADOW
            if (actorRol == Rol.EMPLEADO) {
                auditarRechazo(tipo, actorId, actorCorreo, computadoraId, ipOrigen, "un empleado no puede supervisar");
                return EmisionLanzamiento.rechazo("Solo manager o administrador pueden supervisar (shadow).");
            }
            if (!ocupada) {
                auditarRechazo(tipo, actorId, actorCorreo, computadoraId, ipOrigen, "no hay sesión activa que supervisar");
                return EmisionLanzamiento.rechazo("La computadora no tiene una sesión activa para supervisar.");
            }
            idSesionObjetivo = activa.get().getIdSesionWindows();
        }

        Usuario actor = usuarioRepository.findById(actorId)
                .orElseThrow(() -> new ReglaNegocioException("Usuario inexistente: " + actorId));
        Instant ahora = reloj.instant();
        String token = Tokens.generar();
        Lanzamiento l = new Lanzamiento(actor, c, tipo, Tokens.hash(token), ahora,
                ahora.plus(propiedades.vigencia()), idSesionObjetivo);
        lanzamientoRepository.save(l);

        if (tipo == TipoLanzamiento.CONECTAR) {
            auditoriaService.lanzamientoRdp(actorId, actorCorreo, computadoraId, ipOrigen,
                    "Lanzamiento CONECTAR emitido para " + c.getNombre());
        } else {
            auditoriaService.shadow(actorId, actorCorreo, computadoraId,
                    "Shadow emitido para " + c.getNombre() + " (sesión " + idSesionObjetivo + ")");
        }

        String url = propiedades.getProtocolo() + "://redimir?token=" + token;
        return EmisionLanzamiento.ok(token, propiedades.getProtocolo(), url, tipo, c.getNombre(),
                propiedades.getVigenciaSegundos());
    }

    private void auditarRechazo(TipoLanzamiento tipo, Long actorId, String actorCorreo,
                                Long computadoraId, String ipOrigen, String motivo) {
        if (tipo == TipoLanzamiento.SHADOW) {
            auditoriaService.shadow(actorId, actorCorreo, computadoraId, "Shadow RECHAZADO: " + motivo);
        } else {
            auditoriaService.lanzamientoRdp(actorId, actorCorreo, computadoraId, ipOrigen,
                    "Lanzamiento RECHAZADO: " + motivo);
        }
    }

    /**
     * Canje por el helper (de un solo uso). Devuelve host/cuenta/contraseña/preferencias
     * para CONECTAR, o host/sesión para SHADOW. Lanza ReglaNegocioException si el token
     * no es válido, ya se usó o venció.
     */
    @Transactional
    public CanjeResultado canjear(String token, String ipHelper) {
        if (!StringUtils.hasText(token)) {
            throw new ReglaNegocioException("Falta el token.");
        }
        Lanzamiento l = lanzamientoRepository.findByTokenHash(Tokens.hash(token))
                .orElseThrow(() -> new ReglaNegocioException("Token no válido."));
        if (l.getEstado() != EstadoLanzamiento.EMITIDO) {
            throw new ReglaNegocioException("El token ya fue usado o no está disponible.");
        }
        Instant ahora = reloj.instant();
        if (ahora.isAfter(l.getVenceEn())) {
            l.setEstado(EstadoLanzamiento.EXPIRADO);
            throw new ReglaNegocioException("El token venció.");
        }
        l.setEstado(EstadoLanzamiento.CANJEADO);
        l.setCanjeadoEn(ahora);
        l.setIpHelper(ipHelper);

        Computadora c = l.getComputadora();
        PreferenciasVista p = preferenciasService.obtener(l.getUsuario().getId());
        // La contraseña sale para CONECTAR y también para SHADOW: en PCs de workgroup el
        // shadow necesita autenticarse al destino (cmdkey) antes de lanzar mstsc /shadow.
        String password = null;
        if (c.getCredencialCifrada() != null) {
            password = cifrador.descifrar(c.getCredencialCifrada());
        } else if (l.getTipo() == TipoLanzamiento.CONECTAR) {
            throw new ReglaNegocioException("La PC no tiene contraseña configurada.");
        }
        return new CanjeResultado(
                l.getTipo().name(), c.getDireccionTailscale(), c.getCuentaUsuario(), password,
                l.getIdSesionObjetivo(), c.getNombre(),
                p.ajusteAutomatico(), p.pantallaCompleta(), p.multimonitor(), p.escala(),
                p.sonido().name(), p.microfono(), p.atajos().name(), p.calidad().name());
    }
}
