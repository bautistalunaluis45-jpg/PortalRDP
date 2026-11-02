package com.agentesocial.portalrdp.repositorio;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.agentesocial.portalrdp.dominio.EstadoSesion;
import com.agentesocial.portalrdp.dominio.SesionRdp;

public interface SesionRdpRepository extends JpaRepository<SesionRdp, Long> {

    /** Sesiones abiertas (no finalizadas) de una PC, para reconciliar el latido. */
    List<SesionRdp> findByComputadora_IdAndFinEnIsNull(Long computadoraId);

    /** Sesión conectada actual de una PC (si existe). */
    Optional<SesionRdp> findFirstByComputadora_IdAndEstadoAndFinEnIsNull(Long computadoraId, EstadoSesion estado);

    /** Sesiones conectadas de un conjunto de PCs (para pintar varias filas a la vez). */
    List<SesionRdp> findByComputadora_IdInAndEstadoAndFinEnIsNull(Collection<Long> computadoraIds, EstadoSesion estado);

    /** Todas las sesiones conectadas ahora (para el resumen "en vivo"). */
    List<SesionRdp> findByEstadoAndFinEnIsNullOrderByDesdeAsc(EstadoSesion estado);

    /** Borrado en cascada al eliminar una computadora por completo. */
    @Modifying
    @Query("delete from SesionRdp s where s.computadora.id = :computadoraId")
    void eliminarPorComputadora(@Param("computadoraId") Long computadoraId);

    /** Al eliminar un usuario: se deja la sesión (histórico de la PC) pero sin atribución. */
    @Modifying
    @Query("update SesionRdp s set s.usuarioAtribuidoId = null where s.usuarioAtribuidoId = :usuarioId")
    void desatribuirUsuario(@Param("usuarioId") Long usuarioId);
}
