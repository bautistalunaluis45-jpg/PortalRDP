package com.agentesocial.portalrdp.repositorio;

import java.time.Instant;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.agentesocial.portalrdp.dominio.AccesoTemporal;

/**
 * Vigente = no revocado Y (sin vencimiento O aún no vencido). Un vencimiento
 * nulo representa un acceso indefinido (hasta revocación manual).
 */
public interface AccesoTemporalRepository extends JpaRepository<AccesoTemporal, Long> {

    @Query("""
            select a from AccesoTemporal a
            where a.usuario.id = :usuarioId
              and a.revocadoEn is null
              and (a.venceEn is null or a.venceEn > :ahora)
            order by a.venceEn asc
            """)
    List<AccesoTemporal> vigentesDeUsuario(@Param("usuarioId") Long usuarioId, @Param("ahora") Instant ahora);

    @Query("""
            select a from AccesoTemporal a
            where a.revocadoEn is null
              and (a.venceEn is null or a.venceEn > :ahora)
            order by a.venceEn asc
            """)
    List<AccesoTemporal> vigentes(@Param("ahora") Instant ahora);

    @Query("""
            select (count(a) > 0) from AccesoTemporal a
            where a.usuario.id = :usuarioId
              and a.computadora.id = :computadoraId
              and a.revocadoEn is null
              and (a.venceEn is null or a.venceEn > :ahora)
            """)
    boolean existeVigente(@Param("usuarioId") Long usuarioId,
                          @Param("computadoraId") Long computadoraId,
                          @Param("ahora") Instant ahora);

    /** Borrado en cascada al eliminar una computadora por completo. */
    @Modifying
    @Query("delete from AccesoTemporal a where a.computadora.id = :computadoraId")
    void eliminarPorComputadora(@Param("computadoraId") Long computadoraId);

    /** Borrado al eliminar un usuario: sus accesos y los que él otorgó a otros. */
    @Modifying
    @Query("delete from AccesoTemporal a where a.usuario.id = :usuarioId or a.otorgadoPor.id = :usuarioId")
    void eliminarPorUsuarioOOtorgante(@Param("usuarioId") Long usuarioId);
}
