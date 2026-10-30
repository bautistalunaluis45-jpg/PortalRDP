package com.agentesocial.portalrdp.seguridad;

import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.agentesocial.portalrdp.dominio.Usuario;
import com.agentesocial.portalrdp.excepcion.AccesoDenegadoException;
import com.agentesocial.portalrdp.servicio.AuditoriaService;
import com.agentesocial.portalrdp.servicio.UsuarioService;

/**
 * Valida el login con Google y lo liga a un usuario del portal.
 * Reglas: el correo debe estar verificado por Google y debe existir un usuario
 * activo con ese correo (el alta la hace el admin; Google no da de alta). La
 * restriccion por dominio es opcional (ver PropiedadesAutenticacion): si no se
 * configura, cualquier dominio es aceptable y manda el alta previa.
 */
@Service
public class OidcUsuarioService implements OAuth2UserService<OidcUserRequest, OidcUser> {

    private final OidcUserService delegado = new OidcUserService();
    private final UsuarioService usuarioService;
    private final PropiedadesAutenticacion propiedades;
    private final AuditoriaService auditoriaService;

    public OidcUsuarioService(UsuarioService usuarioService, PropiedadesAutenticacion propiedades,
                              AuditoriaService auditoriaService) {
        this.usuarioService = usuarioService;
        this.propiedades = propiedades;
        this.auditoriaService = auditoriaService;
    }

    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        OidcUser oidc = delegado.loadUser(userRequest);

        String correo = oidc.getEmail();
        if (correo == null || !Boolean.TRUE.equals(oidc.getEmailVerified())) {
            throw error("correo_no_verificado", "El correo de Google no esta verificado.");
        }
        if (!propiedades.dominioPermitido(correo)) {
            throw error("dominio_no_permitido", "El dominio del correo no esta autorizado.");
        }

        try {
            Usuario usuario = usuarioService.registrarIngresoGoogle(correo, oidc.getSubject());
            auditoriaService.entradaPortal(usuario.getId(), usuario.getCorreo(), ipDeLaSolicitud());
            return new UsuarioOidcPrincipal(oidc, usuario);
        } catch (AccesoDenegadoException ex) {
            throw error("usuario_no_registrado", ex.getMessage());
        }
    }

    private static String ipDeLaSolicitud() {
        try {
            if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs) {
                return attrs.getRequest().getRemoteAddr();
            }
        } catch (RuntimeException ignorado) {
            // sin contexto de solicitud: se omite la IP
        }
        return null;
    }

    private static OAuth2AuthenticationException error(String codigo, String descripcion) {
        return new OAuth2AuthenticationException(new OAuth2Error(codigo, descripcion, null));
    }
}
