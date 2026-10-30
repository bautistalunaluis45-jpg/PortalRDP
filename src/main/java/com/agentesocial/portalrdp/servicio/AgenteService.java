package com.agentesocial.portalrdp.servicio;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.agentesocial.portalrdp.api.dto.LatidoRequest;
import com.agentesocial.portalrdp.api.dto.LatidoResponse;
import com.agentesocial.portalrdp.api.dto.SesionDto;
import com.agentesocial.portalrdp.dominio.Computadora;
import com.agentesocial.portalrdp.dominio.EstadoSesion;
import com.agentesocial.portalrdp.dominio.SesionRdp;
import com.agentesocial.portalrdp.excepcion.AgenteNoAutorizadoException;
import com.agentesocial.portalrdp.infraestructura.Tokens;
import com.agentesocial.portalrdp.repositorio.ComputadoraRepository;
import com.agentesocial.portalrdp.repositorio.SesionRdpRepository;

/**
 * Procesa el latido del agente de una PC: autentica por token por PC (hash),
 * actualiza el último latido y reconcilia las sesiones reportadas (docs/decisiones.md 8, 12).
 */
@Service
public class AgenteService {

    private final ComputadoraRepository computadoraRepository;
    private final SesionRdpRepository sesionRepository;
    private final AuditoriaService auditoriaService;
    private final Clock reloj;

    public AgenteService(ComputadoraRepository computadoraRepository,
                         SesionRdpRepository sesionRepository,
                         AuditoriaService auditoriaService,
                         Clock reloj) {
        this.computadoraRepository = computadoraRepository;
        this.sesionRepository = sesionRepository;
        this.auditoriaService = auditoriaService;
        this.reloj = reloj;
    }

    @Transactional
    public LatidoResponse procesarLatido(String tokenPlano, LatidoRequest request) {
        if (!StringUtils.hasText(tokenPlano)) {
            throw new AgenteNoAutorizadoException("Falta el token del agente.");
        }
        Computadora computadora = computadoraRepository.findByTokenAgenteHash(Tokens.hash(tokenPlano))
                .orElseThrow(() -> new AgenteNoAutorizadoException("Token de agente no válido."));

        Instant ahora = reloj.instant();
        computadora.setUltimoLatido(ahora);

        List<SesionDto> reportadas = request == null || request.sesiones() == null ? List.of() : request.sesiones();
        Map<String, SesionRdp> abiertas = sesionRepository.findByComputadora_IdAndFinEnIsNull(computadora.getId())
                .stream().collect(Collectors.toMap(SesionRdp::getIdSesionWindows, Function.identity(), (a, b) -> a));
        Set<String> reportadasIds = new HashSet<>();

        for (SesionDto dto : reportadas) {
            if (!StringUtils.hasText(dto.idSesionWindows())) {
                continue;
            }
            reportadasIds.add(dto.idSesionWindows());
            EstadoSesion estado = parseEstado(dto.estado());
            SesionRdp sesion = abiertas.get(dto.idSesionWindows());
            if (sesion == null) {
                Instant desde = dto.desde() != null ? dto.desde() : ahora;
                sesion = new SesionRdp(computadora, dto.idSesionWindows(), estado, desde);
            } else {
                sesion.setEstado(estado);
            }
            sesion.setUsuarioWindows(dto.usuarioWindows());
            sesion.setIpOrigen(dto.ipOrigen());
            sesion.setIdleSegundos(dto.idleSegundos() == null ? 0 : Math.max(0, dto.idleSegundos()));
            sesionRepository.save(sesion);
        }

        // Sesiones que ya no reporta el agente: se cerraron (logoff).
        for (SesionRdp abierta : abiertas.values()) {
            if (!reportadasIds.contains(abierta.getIdSesionWindows())) {
                abierta.setFinEn(ahora);
                long minutos = Duration.between(abierta.getDesde(), ahora).toMinutes();
                auditoriaService.finSesion(computadora.getId(),
                        "Fin de sesión " + abierta.getIdSesionWindows() + " en " + computadora.getNombre()
                                + " (duración " + minutos + " min)");
            }
        }

        return new LatidoResponse(true, reportadasIds.size());
    }

    private static EstadoSesion parseEstado(String estado) {
        if ("DESCONECTADA".equalsIgnoreCase(estado)) {
            return EstadoSesion.DESCONECTADA;
        }
        return EstadoSesion.ACTIVA;
    }
}
