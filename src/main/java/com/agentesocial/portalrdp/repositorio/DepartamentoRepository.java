package com.agentesocial.portalrdp.repositorio;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.agentesocial.portalrdp.dominio.Departamento;

public interface DepartamentoRepository extends JpaRepository<Departamento, Long> {

    List<Departamento> findAllByOrderByNombreAsc();

    boolean existsByNombreIgnoreCase(String nombre);

    boolean existsByNombreIgnoreCaseAndIdNot(String nombre, Long id);

    boolean existsByCodigoIgnoreCase(String codigo);

    boolean existsByCodigoIgnoreCaseAndIdNot(String codigo, Long id);
}
