package com.agentesocial.portalrdp.repositorio;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.agentesocial.portalrdp.dominio.EstadoOrden;
import com.agentesocial.portalrdp.dominio.OrdenAgente;

public interface OrdenAgenteRepository extends JpaRepository<OrdenAgente, Long> {

    List<OrdenAgente> findByComputadora_IdAndEstadoOrderByCreadaEnAsc(Long computadoraId, EstadoOrden estado);

    /** Borrado en cascada al eliminar una computadora por completo. */
    @Modifying
    @Query("delete from OrdenAgente o where o.computadora.id = :computadoraId")
    void eliminarPorComputadora(@Param("computadoraId") Long computadoraId);

    /** Borrado en cascada al eliminar un usuario por completo (órdenes que creó). */
    @Modifying
    @Query("delete from OrdenAgente o where o.creadaPor.id = :usuarioId")
    void eliminarPorCreador(@Param("usuarioId") Long usuarioId);
}
