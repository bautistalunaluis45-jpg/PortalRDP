package com.agentesocial.portalrdp.repositorio;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.agentesocial.portalrdp.dominio.Computadora;

public interface ComputadoraRepository extends JpaRepository<Computadora, Long> {

    Optional<Computadora> findByTokenAgenteHash(String tokenAgenteHash);

    List<Computadora> findByDepartamento_IdAndActivaTrueOrderByNombreAsc(Long departamentoId);

    long countByDepartamento_Id(Long departamentoId);

    List<Computadora> findAllByOrderByNombreAsc();

    boolean existsByNombreIgnoreCase(String nombre);

    boolean existsByNombreIgnoreCaseAndIdNot(String nombre, Long id);

    boolean existsByDireccionTailscaleIgnoreCase(String direccionTailscale);

    boolean existsByDireccionTailscaleIgnoreCaseAndIdNot(String direccionTailscale, Long id);
}
