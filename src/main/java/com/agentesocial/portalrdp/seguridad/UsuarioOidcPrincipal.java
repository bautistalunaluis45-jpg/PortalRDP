package com.agentesocial.portalrdp.seguridad;

import java.util.Collection;
import java.util.Map;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

import com.agentesocial.portalrdp.dominio.Rol;
import com.agentesocial.portalrdp.dominio.Usuario;

/**
 * Principal del login con Google: envuelve al OidcUser de Google y le agrega la
 * identidad y el rol del usuario del portal. Las autoridades salen del rol del
 * portal, no de Google.
 */
public class UsuarioOidcPrincipal implements OidcUser, PrincipalPortal {

    private final OidcUser delegado;
    private final Long usuarioId;
    private final String nombre;
    private final String correo;
    private final Rol rol;
    private final Collection<GrantedAuthority> autoridades;

    public UsuarioOidcPrincipal(OidcUser delegado, Usuario usuario) {
        this.delegado = delegado;
        this.usuarioId = usuario.getId();
        this.nombre = usuario.getNombre();
        this.correo = usuario.getCorreo();
        this.rol = usuario.getRol();
        this.autoridades = PrincipalPortal.autoridadesDe(usuario);
    }

    // --- PrincipalPortal ---

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

    // --- OidcUser / OAuth2User ---

    @Override
    public Collection<GrantedAuthority> getAuthorities() {
        return autoridades;
    }

    @Override
    public Map<String, Object> getAttributes() {
        return delegado.getAttributes();
    }

    @Override
    public Map<String, Object> getClaims() {
        return delegado.getClaims();
    }

    @Override
    public OidcUserInfo getUserInfo() {
        return delegado.getUserInfo();
    }

    @Override
    public OidcIdToken getIdToken() {
        return delegado.getIdToken();
    }

    @Override
    public String getName() {
        return delegado.getName();
    }
}
