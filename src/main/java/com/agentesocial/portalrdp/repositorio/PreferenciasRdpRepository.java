package com.agentesocial.portalrdp.repositorio;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.agentesocial.portalrdp.dominio.PreferenciasRdp;

public interface PreferenciasRdpRepository extends JpaRepository<PreferenciasRdp, Long> {

    Optional<PreferenciasRdp> findByUsuarioId(Long usuarioId);

    /** Borrado en cascada al eliminar un usuario por completo. */
    @Modifying
    @Query("delete from PreferenciasRdp p where p.usuarioId = :usuarioId")
    void eliminarPorUsuario(@Param("usuarioId") Long usuarioId);
}
