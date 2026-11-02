package com.agentesocial.portalrdp.repositorio;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.agentesocial.portalrdp.dominio.EstadoOrden;
import com.agentesocial.portalrdp.dominio.OrdenAgente;

public interface OrdenAgenteRepository extends JpaRepository<OrdenAgente, Long> {

    List<OrdenAgente> findByComputadora_IdAndEstadoOrderByCreadaEnAsc(Long computadoraId, EstadoOrden estado);
}
