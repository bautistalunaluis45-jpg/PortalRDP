package com.agentesocial.portalrdp.seguridad.dev;

import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.agentesocial.portalrdp.dominio.Usuario;
import com.agentesocial.portalrdp.seguridad.PrincipalPortal;
import com.agentesocial.portalrdp.servicio.AuditoriaService;
import com.agentesocial.portalrdp.servicio.UsuarioService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Login simulado SOLO para desarrollo (perfil "dev"). Muestra los usuarios
 * activos y permite iniciar sesion como cualquiera de ellos sin Google ni
 * contrasena. No existe en produccion.
 */
@Controller
@Profile("dev")
public class DevLoginController {

    private final UsuarioService usuarioService;
    private final SecurityContextRepository securityContextRepository;
    private final AuditoriaService auditoriaService;

    public DevLoginController(UsuarioService usuarioService,
                              SecurityContextRepository securityContextRepository,
                              AuditoriaService auditoriaService) {
        this.usuarioService = usuarioService;
        this.securityContextRepository = securityContextRepository;
        this.auditoriaService = auditoriaService;
    }

    @GetMapping("/dev/login")
    public String formulario(Model model) {
        model.addAttribute("usuarios", usuarioService.listarActivos());
        return "dev/login";
    }

    @PostMapping("/dev/login")
    public String entrar(@RequestParam String correo,
                         HttpServletRequest request,
                         HttpServletResponse response) {
        Usuario usuario = usuarioService.buscarActivoPorCorreo(correo)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no valido: " + correo));

        PrincipalDev principal = new PrincipalDev(usuario);
        Authentication autenticacion = new UsernamePasswordAuthenticationToken(
                principal, null, PrincipalPortal.autoridadesDe(usuario));

        SecurityContext contexto = SecurityContextHolder.createEmptyContext();
        contexto.setAuthentication(autenticacion);
        SecurityContextHolder.setContext(contexto);
        securityContextRepository.saveContext(contexto, request, response);

        auditoriaService.entradaPortal(usuario.getId(), usuario.getCorreo(), request.getRemoteAddr());
        return "redirect:/web/";
    }
}
