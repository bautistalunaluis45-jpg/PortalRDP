package com.agentesocial.portalrdp.repositorio;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.agentesocial.portalrdp.dominio.EventoAuditoria;
import com.agentesocial.portalrdp.dominio.TipoEventoAuditoria;

/**
 * Repositorio de auditoria. Insercion y consulta; nunca update ni delete (la
 * tabla es de solo insercion). La pantalla A10 usa filtros via Specifications.
 */
public interface EventoAuditoriaRepository
        extends JpaRepository<EventoAuditoria, Long>, JpaSpecificationExecutor<EventoAuditoria> {

    long countByOcurridoEnGreaterThanEqual(Instant desde);

    long countByTipo(TipoEventoAuditoria tipo);

    /** Ultimos 5 eventos en los que el usuario fue actor o afectado (para el detalle). */
    List<EventoAuditoria> findTop5ByActorUsuarioIdOrUsuarioAfectadoIdOrderByOcurridoEnDesc(
            Long actorUsuarioId, Long usuarioAfectadoId);

    /** Todo el historial del usuario (actor o afectado), mas recientes primero. */
    List<EventoAuditoria> findByActorUsuarioIdOrUsuarioAfectadoIdOrderByOcurridoEnDesc(
            Long actorUsuarioId, Long usuarioAfectadoId);

    /** Ultimo evento de un tipo para un actor (p. ej. ultima ENTRADA_PORTAL = ultimo acceso). */
    Optional<EventoAuditoria> findFirstByActorUsuarioIdAndTipoOrderByOcurridoEnDesc(
            Long actorUsuarioId, TipoEventoAuditoria tipo);
}
