package com.agentesocial.portalrdp.repositorio;

import java.time.Instant;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
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
}
