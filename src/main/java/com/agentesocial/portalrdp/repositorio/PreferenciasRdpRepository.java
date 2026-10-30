package com.agentesocial.portalrdp.repositorio;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.agentesocial.portalrdp.dominio.PreferenciasRdp;

public interface PreferenciasRdpRepository extends JpaRepository<PreferenciasRdp, Long> {

    Optional<PreferenciasRdp> findByUsuarioId(Long usuarioId);
}
