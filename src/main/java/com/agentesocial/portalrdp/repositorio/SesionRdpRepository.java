package com.agentesocial.portalrdp.repositorio;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.agentesocial.portalrdp.dominio.EstadoSesion;
import com.agentesocial.portalrdp.dominio.SesionRdp;

public interface SesionRdpRepository extends JpaRepository<SesionRdp, Long> {

    /** Sesiones abiertas (no finalizadas) de una PC, para reconciliar el latido. */
    List<SesionRdp> findByComputadora_IdAndFinEnIsNull(Long computadoraId);

    /** Sesión conectada actual de una PC (si existe). */
    Optional<SesionRdp> findFirstByComputadora_IdAndEstadoAndFinEnIsNull(Long computadoraId, EstadoSesion estado);

    /** Sesiones conectadas de un conjunto de PCs (para pintar varias filas a la vez). */
    List<SesionRdp> findByComputadora_IdInAndEstadoAndFinEnIsNull(Collection<Long> computadoraIds, EstadoSesion estado);
}
