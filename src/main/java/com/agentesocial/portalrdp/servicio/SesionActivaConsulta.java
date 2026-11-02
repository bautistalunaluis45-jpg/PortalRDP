package com.agentesocial.portalrdp.servicio;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.agentesocial.portalrdp.dominio.EstadoSesion;
import com.agentesocial.portalrdp.dominio.SesionRdp;
import com.agentesocial.portalrdp.dominio.Usuario;
import com.agentesocial.portalrdp.repositorio.SesionRdpRepository;
import com.agentesocial.portalrdp.repositorio.UsuarioRepository;

/**
 * Ayudante (no es bean) para consultar las sesiones CONECTADAS y decidir si una PC
 * está OCUPADA y por quién. Lo usan VisibilidadService, ComputadoraService y ResumenService.
 */
class SesionActivaConsulta {

    private final SesionRdpRepository repositorio;
    private final UsuarioRepository usuarioRepository;

    SesionActivaConsulta(SesionRdpRepository repositorio, UsuarioRepository usuarioRepository) {
        this.repositorio = repositorio;
        this.usuarioRepository = usuarioRepository;
    }

    /** Mapa computadoraId -> sesión conectada (una por PC) para un conjunto de PCs. */
    Map<Long, SesionRdp> mapaActivas(Collection<Long> computadoraIds) {
        if (computadoraIds.isEmpty()) {
            return Map.of();
        }
        List<SesionRdp> activas = repositorio
                .findByComputadora_IdInAndEstadoAndFinEnIsNull(computadoraIds, EstadoSesion.ACTIVA);
        return activas.stream().collect(Collectors.toMap(
                s -> s.getComputadora().getId(), Function.identity(), (a, b) -> a));
    }

    boolean estaOcupada(Long computadoraId) {
        return repositorio
                .findFirstByComputadora_IdAndEstadoAndFinEnIsNull(computadoraId, EstadoSesion.ACTIVA)
                .isPresent();
    }

    /**
     * Nombre a mostrar de quien usa la PC: prefiere el usuario del PORTAL atribuido (por la
     * unión por IP del lanzamiento); si no hay, cae al usuario de Windows reportado, o "desconocido".
     */
    String nombreMostrado(SesionRdp sesion) {
        if (sesion == null) {
            return null;
        }
        if (sesion.getUsuarioAtribuidoId() != null) {
            String nombre = usuarioRepository.findById(sesion.getUsuarioAtribuidoId())
                    .map(Usuario::getNombre).orElse(null);
            if (nombre != null && !nombre.isBlank()) {
                return nombre;
            }
        }
        return nombreEnUso(sesion);
    }

    /** Nombre crudo del usuario de Windows reportado, o "desconocido" (sin atribución del portal). */
    static String nombreEnUso(SesionRdp sesion) {
        if (sesion == null) {
            return null;
        }
        return sesion.getUsuarioWindows() != null && !sesion.getUsuarioWindows().isBlank()
                ? sesion.getUsuarioWindows() : "desconocido";
    }
}
