package com.agentesocial.portalrdp.servicio;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.agentesocial.portalrdp.dominio.Computadora;
import com.agentesocial.portalrdp.dominio.Departamento;
import com.agentesocial.portalrdp.dominio.EstadoComputadora;
import com.agentesocial.portalrdp.excepcion.ReglaNegocioException;
import com.agentesocial.portalrdp.infraestructura.Cifrador;
import com.agentesocial.portalrdp.infraestructura.PropiedadesEstado;
import com.agentesocial.portalrdp.repositorio.ComputadoraRepository;
import com.agentesocial.portalrdp.repositorio.DepartamentoRepository;

/** Alta, edición y baja de computadoras (A5, A6, A7). La contraseña se guarda cifrada y nunca se muestra. */
@Service
public class ComputadoraService {

    private static final DateTimeFormatter FORMATO = DateTimeFormatter.ofPattern("d MMM, HH:mm", new Locale("es"));

    private final ComputadoraRepository computadoraRepository;
    private final DepartamentoRepository departamentoRepository;
    private final Cifrador cifrador;
    private final Clock reloj;
    private final PropiedadesEstado propiedadesEstado;
    private final AuditoriaService auditoriaService;
    private final SesionActivaConsulta sesiones;

    public ComputadoraService(ComputadoraRepository computadoraRepository,
                              DepartamentoRepository departamentoRepository,
                              Cifrador cifrador,
                              Clock reloj,
                              PropiedadesEstado propiedadesEstado,
                              AuditoriaService auditoriaService,
                              com.agentesocial.portalrdp.repositorio.SesionRdpRepository sesionRdpRepository) {
        this.computadoraRepository = computadoraRepository;
        this.departamentoRepository = departamentoRepository;
        this.cifrador = cifrador;
        this.reloj = reloj;
        this.propiedadesEstado = propiedadesEstado;
        this.auditoriaService = auditoriaService;
        this.sesiones = new SesionActivaConsulta(sesionRdpRepository);
    }

    @Transactional(readOnly = true)
    public List<ComputadoraAdminVista> listar() {
        Instant ahora = reloj.instant();
        Duration umbral = propiedadesEstado.umbralSinConexion();
        List<Computadora> todas = computadoraRepository.findAllByOrderByNombreAsc();
        java.util.Map<Long, com.agentesocial.portalrdp.dominio.SesionRdp> activas =
                sesiones.mapaActivas(todas.stream().map(Computadora::getId).toList());
        List<ComputadoraAdminVista> vistas = new ArrayList<>();
        for (Computadora c : todas) {
            com.agentesocial.portalrdp.dominio.SesionRdp sesion = activas.get(c.getId());
            EstadoComputadora est = estado(c, ahora, umbral, sesion != null);
            boolean ocupada = est == EstadoComputadora.OCUPADA;
            vistas.add(new ComputadoraAdminVista(
                    c.getId(), c.getNombre(), c.getDireccionTailscale(), c.getDepartamento().getNombre(),
                    est,
                    ocupada ? SesionActivaConsulta.nombreEnUso(sesion) : null,
                    ocupada ? idleTexto(sesion.getIdleSegundos()) : null,
                    textoLatido(c.getUltimoLatido(), ahora),
                    c.getCredencialCifrada() != null, c.isActiva()));
        }
        return vistas;
    }

    @Transactional(readOnly = true)
    public ComputadorasResumen resumen() {
        Instant ahora = reloj.instant();
        Duration umbral = propiedadesEstado.umbralSinConexion();
        long libres = 0, ocupadas = 0, sinConexion = 0, sinCredencial = 0;
        List<Computadora> todas = computadoraRepository.findAllByOrderByNombreAsc();
        java.util.Map<Long, com.agentesocial.portalrdp.dominio.SesionRdp> activas =
                sesiones.mapaActivas(todas.stream().map(Computadora::getId).toList());
        for (Computadora c : todas) {
            switch (estado(c, ahora, umbral, activas.containsKey(c.getId()))) {
                case LIBRE -> libres++;
                case OCUPADA -> ocupadas++;
                case SIN_CONEXION -> sinConexion++;
            }
            if (c.getCredencialCifrada() == null) {
                sinCredencial++;
            }
        }
        return new ComputadorasResumen(todas.size(), libres, ocupadas, sinConexion, sinCredencial);
    }

    @Transactional(readOnly = true)
    public ComputadoraDetalle obtener(Long id) {
        Computadora c = cargar(id);
        Instant ahora = reloj.instant();
        return new ComputadoraDetalle(c.getId(), c.getNombre(), c.getDireccionTailscale(), c.getCuentaUsuario(),
                c.getDepartamento().getId(), c.getDepartamento().getNombre(),
                estado(c, ahora, propiedadesEstado.umbralSinConexion(), sesiones.estaOcupada(id)),
                textoLatido(c.getUltimoLatido(), ahora), c.getCredencialCifrada() != null,
                c.getTokenAgenteHash() != null, c.isActiva());
    }

    @Transactional
    public Long crear(String nombre, String direccionTailscale, String cuentaUsuario, Long departamentoId,
                      String passwordPlano, Long actorId, String actorCorreo) {
        String n = limpiar(nombre);
        String dir = limpiar(direccionTailscale);
        String cuenta = limpiar(cuentaUsuario);
        validarObligatorios(n, dir, cuenta);
        if (!StringUtils.hasText(passwordPlano)) {
            throw new ReglaNegocioException("La contraseña de la cuenta es obligatoria al dar de alta la PC.");
        }
        if (computadoraRepository.existsByNombreIgnoreCase(n)) {
            throw new ReglaNegocioException("Ya existe una computadora con ese nombre.");
        }
        if (computadoraRepository.existsByDireccionTailscaleIgnoreCase(dir)) {
            throw new ReglaNegocioException("Ya existe una computadora con esa dirección de Tailscale.");
        }
        Departamento departamento = cargarDepartamento(departamentoId);

        Computadora c = new Computadora(n, departamento, dir);
        c.setCuentaUsuario(cuenta);
        c.setCredencialCifrada(cifrador.cifrar(passwordPlano));
        computadoraRepository.save(c);
        auditoriaService.cambioComputadora(actorId, actorCorreo, c.getId(),
                "Alta de computadora: " + n + " (" + dir + ") depto=" + departamento.getNombre()
                        + "; contraseña capturada");
        return c.getId();
    }

    @Transactional
    public void actualizar(Long id, String nombre, String direccionTailscale, String cuentaUsuario,
                           Long departamentoId, String passwordPlano, Long actorId, String actorCorreo) {
        Computadora c = cargar(id);
        String n = limpiar(nombre);
        String dir = limpiar(direccionTailscale);
        String cuenta = limpiar(cuentaUsuario);
        validarObligatorios(n, dir, cuenta);
        if (computadoraRepository.existsByNombreIgnoreCaseAndIdNot(n, id)) {
            throw new ReglaNegocioException("Ya existe otra computadora con ese nombre.");
        }
        if (computadoraRepository.existsByDireccionTailscaleIgnoreCaseAndIdNot(dir, id)) {
            throw new ReglaNegocioException("Ya existe otra computadora con esa dirección de Tailscale.");
        }
        Departamento departamento = cargarDepartamento(departamentoId);
        String antes = c.getNombre() + " (" + c.getDireccionTailscale() + ") depto="
                + c.getDepartamento().getNombre();
        c.setNombre(n);
        c.setDireccionTailscale(dir);
        c.setCuentaUsuario(cuenta);
        c.setDepartamento(departamento);
        auditoriaService.cambioComputadora(actorId, actorCorreo, id,
                "Edición de computadora: [" + antes + "] -> [" + n + " (" + dir + ") depto="
                        + departamento.getNombre() + "]");
        // La contraseña solo se cambia si se escribió una nueva (campo solo-escritura).
        if (StringUtils.hasText(passwordPlano)) {
            c.setCredencialCifrada(cifrador.cifrar(passwordPlano));
            auditoriaService.cambioPasswordPc(actorId, actorCorreo, id);
        }
    }

    @Transactional
    public void cambiarEstado(Long id, boolean activa, Long actorId, String actorCorreo) {
        Computadora c = cargar(id);
        c.setActiva(activa);
        auditoriaService.cambioComputadora(actorId, actorCorreo, id,
                (activa ? "Reactivación" : "Baja") + " de computadora: " + c.getNombre());
    }

    private EstadoComputadora estado(Computadora c, Instant ahora, Duration umbral, boolean ocupada) {
        return EstadoCalculo.calcular(c.getUltimoLatido(), ahora, umbral, ocupada);
    }

    /** Genera (o regenera) el token del agente de una PC. Devuelve el token en claro UNA sola vez. */
    @Transactional
    public String generarTokenAgente(Long id, Long actorId, String actorCorreo) {
        Computadora c = cargar(id);
        String token = com.agentesocial.portalrdp.infraestructura.Tokens.generar();
        c.setTokenAgenteHash(com.agentesocial.portalrdp.infraestructura.Tokens.hash(token));
        auditoriaService.cambioComputadora(actorId, actorCorreo, id,
                "Token de agente (re)generado para " + c.getNombre());
        return token;
    }

    private String textoLatido(Instant ultimoLatido, Instant ahora) {
        return ultimoLatido == null ? "Sin reporte" : FORMATO.format(ultimoLatido.atZone(reloj.getZone()));
    }

    private static String idleTexto(int idleSegundos) {
        if (idleSegundos <= 0) {
            return "Activo";
        }
        long m = idleSegundos / 60;
        if (m < 60) {
            return m + " min inactivo";
        }
        return (m / 60) + "h " + (m % 60) + "m inactivo";
    }

    private Computadora cargar(Long id) {
        return computadoraRepository.findById(id)
                .orElseThrow(() -> new ReglaNegocioException("La computadora no existe."));
    }

    private Departamento cargarDepartamento(Long departamentoId) {
        if (departamentoId == null) {
            throw new ReglaNegocioException("El departamento es obligatorio.");
        }
        return departamentoRepository.findById(departamentoId)
                .orElseThrow(() -> new ReglaNegocioException("El departamento no existe."));
    }

    private static void validarObligatorios(String nombre, String direccion, String cuenta) {
        if (!StringUtils.hasText(nombre)) {
            throw new ReglaNegocioException("El nombre es obligatorio.");
        }
        if (!StringUtils.hasText(direccion)) {
            throw new ReglaNegocioException("La dirección de Tailscale es obligatoria.");
        }
        if (!StringUtils.hasText(cuenta)) {
            throw new ReglaNegocioException("El usuario de la cuenta es obligatorio.");
        }
    }

    private static String limpiar(String valor) {
        return valor == null ? null : valor.trim();
    }
}
