package com.agentesocial.portalrdp.repositorio;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.agentesocial.portalrdp.dominio.Usuario;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByCorreo(String correo);

    List<Usuario> findByActivoTrueOrderByNombreAsc();

    List<Usuario> findAllByOrderByNombreAsc();

    long countByDepartamento_Id(Long departamentoId);
}
