package com.agentesocial.portalrdp.repositorio;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.agentesocial.portalrdp.dominio.EstadoLanzamiento;
import com.agentesocial.portalrdp.dominio.Lanzamiento;
import com.agentesocial.portalrdp.dominio.TipoLanzamiento;

public interface LanzamientoRepository extends JpaRepository<Lanzamiento, Long> {

    Optional<Lanzamiento> findByTokenHash(String tokenHash);

    /**
     * Último lanzamiento que puede explicar una sesión: CONECTAR canjeado para esa PC
     * con la IP del helper igual a la IP de origen que reporta el agente (docs 8).
     */
    Optional<Lanzamiento> findFirstByComputadora_IdAndTipoAndEstadoAndIpHelperOrderByCanjeadoEnDesc(
            Long computadoraId, TipoLanzamiento tipo, EstadoLanzamiento estado, String ipHelper);
}
