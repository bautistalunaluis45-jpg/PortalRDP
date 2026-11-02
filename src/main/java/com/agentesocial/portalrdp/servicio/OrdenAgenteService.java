package com.agentesocial.portalrdp.servicio;

import java.time.Clock;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.agentesocial.portalrdp.dominio.Computadora;
import com.agentesocial.portalrdp.dominio.EstadoSesion;
import com.agentesocial.portalrdp.dominio.OrdenAgente;
import com.agentesocial.portalrdp.dominio.SesionRdp;
import com.agentesocial.portalrdp.dominio.TipoOrden;
import com.agentesocial.portalrdp.dominio.Usuario;
import com.agentesocial.portalrdp.excepcion.ReglaNegocioException;
import com.agentesocial.portalrdp.repositorio.ComputadoraRepository;
import com.agentesocial.portalrdp.repositorio.OrdenAgenteRepository;
import com.agentesocial.portalrdp.repositorio.SesionRdpRepository;
import com.agentesocial.portalrdp.repositorio.UsuarioRepository;

/**
 * Encola órdenes hacia el agente (fase 5 / §7). Hoy solo DESCONECTAR: desconecta a un
 * usuario sin cerrar su sesión. La ejecuta el agente de la PC destino al recoger la orden.
 */
@Service
public class OrdenAgenteService {

    private final OrdenAgenteRepository ordenRepository;
    private final ComputadoraRepository computadoraRepository;
    private final UsuarioRepository usuarioRepository;
    private final SesionRdpRepository sesionRepository;
    private final AuditoriaService auditoriaService;
    private final Clock reloj;

    public OrdenAgenteService(OrdenAgenteRepository ordenRepository,
                              ComputadoraRepository computadoraRepository,
                              UsuarioRepository usuarioRepository,
                              SesionRdpRepository sesionRepository,
                              AuditoriaService auditoriaService,
                              Clock reloj) {
        this.ordenRepository = ordenRepository;
        this.computadoraRepository = computadoraRepository;
        this.usuarioRepository = usuarioRepository;
        this.sesionRepository = sesionRepository;
        this.auditoriaService = auditoriaService;
        this.reloj = reloj;
    }

    @Transactional
    public OrdenDesconexion ordenarDesconectar(Long actorId, String actorCorreo, Long computadoraId) {
        Computadora c = computadoraRepository.findById(computadoraId)
                .orElseThrow(() -> new ReglaNegocioException("La computadora no existe."));
        Optional<SesionRdp> activa = sesionRepository
                .findFirstByComputadora_IdAndEstadoAndFinEnIsNull(computadoraId, EstadoSesion.ACTIVA);
        if (activa.isEmpty()) {
            throw new ReglaNegocioException("La PC no tiene una sesión activa para desconectar.");
        }
        SesionRdp s = activa.get();
        Usuario actor = usuarioRepository.findById(actorId)
                .orElseThrow(() -> new ReglaNegocioException("Usuario inexistente: " + actorId));

        OrdenAgente orden = new OrdenAgente(c, TipoOrden.DESCONECTAR, s.getIdSesionWindows(),
                s.getUsuarioWindows(), actor, reloj.instant());
        ordenRepository.save(orden);

        String quien = SesionActivaConsulta.nombreEnUso(s);
        auditoriaService.desconectar(actorId, actorCorreo, computadoraId,
                "Orden de desconectar a " + quien + " (sesión " + s.getIdSesionWindows() + ") en " + c.getNombre());
        return new OrdenDesconexion(c.getNombre(), quien);
    }
}
