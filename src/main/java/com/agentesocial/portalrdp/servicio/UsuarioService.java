package com.agentesocial.portalrdp.servicio;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.agentesocial.portalrdp.dominio.Departamento;
import com.agentesocial.portalrdp.dominio.Rol;
import com.agentesocial.portalrdp.dominio.Usuario;
import com.agentesocial.portalrdp.excepcion.AccesoDenegadoException;
import com.agentesocial.portalrdp.excepcion.ReglaNegocioException;
import com.agentesocial.portalrdp.repositorio.ComputadoraRepository;
import com.agentesocial.portalrdp.repositorio.DepartamentoRepository;
import com.agentesocial.portalrdp.repositorio.UsuarioRepository;

/**
 * Casos de uso sobre usuarios: resolver el login (OIDC/dev) y la administracion
 * (alta, edicion, activar/desactivar). Registra auditoria en cada cambio.
 */
@Service
public class UsuarioService {

    private static final Pattern CORREO = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private final UsuarioRepository usuarioRepository;
    private final DepartamentoRepository departamentoRepository;
    private final ComputadoraRepository computadoraRepository;
    private final AuditoriaService auditoriaService;
    private final com.agentesocial.portalrdp.repositorio.PreferenciasRdpRepository preferenciasRdpRepository;
    private final com.agentesocial.portalrdp.repositorio.AccesoTemporalRepository accesoTemporalRepository;
    private final com.agentesocial.portalrdp.repositorio.LanzamientoRepository lanzamientoRepository;
    private final com.agentesocial.portalrdp.repositorio.OrdenAgenteRepository ordenAgenteRepository;
    private final com.agentesocial.portalrdp.repositorio.SesionRdpRepository sesionRdpRepository;

    public UsuarioService(UsuarioRepository usuarioRepository,
                          DepartamentoRepository departamentoRepository,
                          ComputadoraRepository computadoraRepository,
                          AuditoriaService auditoriaService,
                          com.agentesocial.portalrdp.repositorio.PreferenciasRdpRepository preferenciasRdpRepository,
                          com.agentesocial.portalrdp.repositorio.AccesoTemporalRepository accesoTemporalRepository,
                          com.agentesocial.portalrdp.repositorio.LanzamientoRepository lanzamientoRepository,
                          com.agentesocial.portalrdp.repositorio.OrdenAgenteRepository ordenAgenteRepository,
                          com.agentesocial.portalrdp.repositorio.SesionRdpRepository sesionRdpRepository) {
        this.usuarioRepository = usuarioRepository;
        this.departamentoRepository = departamentoRepository;
        this.computadoraRepository = computadoraRepository;
        this.auditoriaService = auditoriaService;
        this.preferenciasRdpRepository = preferenciasRdpRepository;
        this.accesoTemporalRepository = accesoTemporalRepository;
        this.lanzamientoRepository = lanzamientoRepository;
        this.ordenAgenteRepository = ordenAgenteRepository;
        this.sesionRdpRepository = sesionRdpRepository;
    }

    /** Cantidad de computadoras del departamento indicado (métrica para el detalle). */
    @Transactional(readOnly = true)
    public long computadorasEnDepartamento(Long departamentoId) {
        return departamentoId == null ? 0 : computadoraRepository.countByDepartamento_Id(departamentoId);
    }

    public static String normalizarCorreo(String correo) {
        return correo == null ? null : correo.trim().toLowerCase(Locale.ROOT);
    }

    // --- Login ---

    @Transactional(readOnly = true)
    public Optional<Usuario> buscarActivoPorCorreo(String correo) {
        return usuarioRepository.findByCorreo(normalizarCorreo(correo)).filter(Usuario::isActivo);
    }

    @Transactional(readOnly = true)
    public List<Usuario> listarActivos() {
        return usuarioRepository.findByActivoTrueOrderByNombreAsc();
    }

    @Transactional
    public Usuario registrarIngresoGoogle(String correo, String googleSub) {
        String correoNormalizado = normalizarCorreo(correo);
        Usuario usuario = usuarioRepository.findByCorreo(correoNormalizado)
                .filter(Usuario::isActivo)
                .orElseThrow(() -> new AccesoDenegadoException(
                        "No existe un usuario activo con el correo indicado"));
        if (StringUtils.hasText(googleSub) && !googleSub.equals(usuario.getGoogleSub())) {
            usuario.setGoogleSub(googleSub);
        }
        return usuario;
    }

    // --- Administracion (A1, A2, A3) ---

    @Transactional(readOnly = true)
    public List<UsuarioVista> listar() {
        List<UsuarioVista> vistas = new ArrayList<>();
        for (Usuario u : usuarioRepository.findAllByOrderByNombreAsc()) {
            vistas.add(aVista(u));
        }
        return vistas;
    }

    @Transactional(readOnly = true)
    public UsuariosResumen resumen() {
        List<Usuario> todos = usuarioRepository.findAllByOrderByNombreAsc();
        long activos = todos.stream().filter(Usuario::isActivo).count();
        long admins = todos.stream().filter(u -> u.getRol() == Rol.ADMIN).count();
        long departamentos = departamentoRepository.count();
        return new UsuariosResumen(todos.size(), activos, todos.size() - activos, admins, departamentos);
    }

    @Transactional(readOnly = true)
    public UsuarioVista obtener(Long id) {
        return aVista(cargar(id));
    }

    @Transactional
    public Long crear(String nombre, String correo, Long departamentoId, Rol rol,
                      Long actorId, String actorCorreo) {
        String n = limpiar(nombre);
        String c = normalizarCorreo(correo);
        if (!StringUtils.hasText(n)) {
            throw new ReglaNegocioException("El nombre es obligatorio.");
        }
        if (c == null || !CORREO.matcher(c).matches()) {
            throw new ReglaNegocioException("El correo no tiene un formato válido.");
        }
        if (rol == null) {
            throw new ReglaNegocioException("El rol es obligatorio.");
        }
        usuarioRepository.findByCorreo(c).ifPresent(existente -> {
            if (existente.isActivo()) {
                throw new ReglaNegocioException("Ya existe un usuario con ese correo.");
            }
            throw new ReglaNegocioException(
                    "Ya existe un usuario inactivo con ese correo; actívalo desde su detalle.");
        });
        Departamento departamento = cargarDepartamento(departamentoId);

        Usuario usuario = new Usuario(n, c, departamento, rol);
        usuarioRepository.save(usuario);
        auditoriaService.cambioUsuario(actorId, actorCorreo, usuario.getId(),
                "Alta de usuario: " + n + " <" + c + "> rol=" + rol + " depto=" + departamento.getNombre());
        return usuario.getId();
    }

    @Transactional
    public void actualizar(Long id, String nombre, Long departamentoId, Rol rol,
                           Long actorId, String actorCorreo) {
        Usuario usuario = cargar(id);
        String n = limpiar(nombre);
        if (!StringUtils.hasText(n)) {
            throw new ReglaNegocioException("El nombre es obligatorio.");
        }
        if (rol == null) {
            throw new ReglaNegocioException("El rol es obligatorio.");
        }
        Departamento departamento = cargarDepartamento(departamentoId);
        String antes = usuario.getNombre() + " / " + usuario.getRol() + " / "
                + usuario.getDepartamento().getNombre();
        usuario.setNombre(n);
        usuario.setRol(rol);
        usuario.setDepartamento(departamento);
        auditoriaService.cambioUsuario(actorId, actorCorreo, id,
                "Edición de usuario: [" + antes + "] -> [" + n + " / " + rol + " / "
                        + departamento.getNombre() + "]");
    }

    @Transactional
    public void cambiarEstado(Long id, boolean activo, Long actorId, String actorCorreo) {
        if (!activo && id.equals(actorId)) {
            throw new ReglaNegocioException("No puedes desactivar tu propia cuenta.");
        }
        Usuario usuario = cargar(id);
        usuario.setActivo(activo);
        auditoriaService.cambioUsuario(actorId, actorCorreo, id,
                (activo ? "Reactivación" : "Baja") + " de usuario: " + usuario.getCorreo());
    }

    /**
     * Elimina DEFINITIVAMENTE un usuario y sus datos operativos asociados: preferencias,
     * accesos temporales (los suyos y los que otorgó a otros), lanzamientos y órdenes que
     * creó. Las sesiones de las PCs se conservan, pero pierden la atribución a este usuario.
     * La bitácora de auditoría NO se borra: queda como historial (la FK se quitó en V11) y se
     * añade un evento del propio borrado. Acción irreversible (A2 "dar de baja" sigue siendo
     * el camino reversible; esto es el borrado total).
     */
    @Transactional
    public void eliminar(Long id, Long actorId, String actorCorreo) {
        if (id.equals(actorId)) {
            throw new ReglaNegocioException("No puedes eliminar tu propia cuenta.");
        }
        Usuario usuario = cargar(id);
        // Auditoría primero: queda constancia con el correo aunque la fila desaparezca.
        auditoriaService.usuarioEliminado(actorId, actorCorreo, id,
                "Eliminación definitiva de usuario: " + usuario.getCorreo());
        // Limpiar lo que lo referencia por clave foránea (datos operativos, no auditoría).
        preferenciasRdpRepository.eliminarPorUsuario(id);
        accesoTemporalRepository.eliminarPorUsuarioOOtorgante(id);
        lanzamientoRepository.eliminarPorUsuario(id);
        ordenAgenteRepository.eliminarPorCreador(id);
        sesionRdpRepository.desatribuirUsuario(id);
        usuarioRepository.delete(usuario);
    }

    private Usuario cargar(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new ReglaNegocioException("El usuario no existe."));
    }

    private Departamento cargarDepartamento(Long departamentoId) {
        if (departamentoId == null) {
            throw new ReglaNegocioException("El departamento es obligatorio.");
        }
        return departamentoRepository.findById(departamentoId)
                .orElseThrow(() -> new ReglaNegocioException("El departamento no existe."));
    }

    private static UsuarioVista aVista(Usuario u) {
        return new UsuarioVista(u.getId(), u.getNombre(), u.getCorreo(),
                u.getDepartamento().getId(), u.getDepartamento().getNombre(), u.getRol(), u.isActivo());
    }

    private static String limpiar(String valor) {
        return valor == null ? null : valor.trim();
    }
}
