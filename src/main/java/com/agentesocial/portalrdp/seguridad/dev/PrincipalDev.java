package com.agentesocial.portalrdp.seguridad.dev;

import com.agentesocial.portalrdp.dominio.Rol;
import com.agentesocial.portalrdp.dominio.Usuario;
import com.agentesocial.portalrdp.seguridad.PrincipalPortal;

/** Principal del login simulado de desarrollo. Solo se usa con el perfil "dev". */
public class PrincipalDev implements PrincipalPortal {

    private final Long usuarioId;
    private final String nombre;
    private final String correo;
    private final Rol rol;

    public PrincipalDev(Usuario usuario) {
        this.usuarioId = usuario.getId();
        this.nombre = usuario.getNombre();
        this.correo = usuario.getCorreo();
        this.rol = usuario.getRol();
    }

    @Override
    public Long getUsuarioId() {
        return usuarioId;
    }

    @Override
    public String getNombre() {
        return nombre;
    }

    @Override
    public String getCorreo() {
        return correo;
    }

    @Override
    public Rol getRol() {
        return rol;
    }
}
