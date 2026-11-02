package com.agentesocial.portalrdp.repositorio;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    /** Borrado en cascada al eliminar una computadora por completo. */
    @Modifying
    @Query("delete from Lanzamiento l where l.computadora.id = :computadoraId")
    void eliminarPorComputadora(@Param("computadoraId") Long computadoraId);

    /** Borrado en cascada al eliminar un usuario por completo. */
    @Modifying
    @Query("delete from Lanzamiento l where l.usuario.id = :usuarioId")
    void eliminarPorUsuario(@Param("usuarioId") Long usuarioId);
}
