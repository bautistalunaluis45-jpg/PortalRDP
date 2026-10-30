package com.agentesocial.portalrdp.seguridad;

import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import com.agentesocial.portalrdp.dominio.Rol;
import com.agentesocial.portalrdp.dominio.Usuario;

/**
 * Datos del usuario autenticado que usa la capa web, sin importar como inicio
 * sesion (Google en produccion, login simulado en dev). Las dos variantes de
 * principal implementan esta interfaz para que los controladores sean uniformes.
 */
public interface PrincipalPortal {

    Long getUsuarioId();

    String getNombre();

    String getCorreo();

    Rol getRol();

    /** Autoridad de Spring Security derivada del rol: ROLE_EMPLEADO, ROLE_MANAGER, ROLE_ADMIN. */
    static Collection<GrantedAuthority> autoridadesDe(Usuario usuario) {
        return List.of(new SimpleGrantedAuthority("ROLE_" + usuario.getRol().name()));
    }
}
