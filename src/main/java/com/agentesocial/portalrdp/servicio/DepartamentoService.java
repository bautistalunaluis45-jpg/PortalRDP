package com.agentesocial.portalrdp.servicio;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.agentesocial.portalrdp.dominio.Departamento;
import com.agentesocial.portalrdp.excepcion.ReglaNegocioException;
import com.agentesocial.portalrdp.repositorio.ComputadoraRepository;
import com.agentesocial.portalrdp.repositorio.DepartamentoRepository;
import com.agentesocial.portalrdp.repositorio.UsuarioRepository;

/** Alta, edicion y baja de departamentos (casos A4). Registra auditoria en cada cambio. */
@Service
public class DepartamentoService {

    private final DepartamentoRepository departamentoRepository;
    private final ComputadoraRepository computadoraRepository;
    private final UsuarioRepository usuarioRepository;
    private final AuditoriaService auditoriaService;

    public DepartamentoService(DepartamentoRepository departamentoRepository,
                               ComputadoraRepository computadoraRepository,
                               UsuarioRepository usuarioRepository,
                               AuditoriaService auditoriaService) {
        this.departamentoRepository = departamentoRepository;
        this.computadoraRepository = computadoraRepository;
        this.usuarioRepository = usuarioRepository;
        this.auditoriaService = auditoriaService;
    }

    @Transactional(readOnly = true)
    public List<DepartamentoVista> listar() {
        List<DepartamentoVista> vistas = new ArrayList<>();
        for (Departamento d : departamentoRepository.findAllByOrderByNombreAsc()) {
            long pcs = computadoraRepository.countByDepartamento_Id(d.getId());
            long usuarios = usuarioRepository.countByDepartamento_Id(d.getId());
            vistas.add(new DepartamentoVista(d.getId(), d.getNombre(), d.getCodigo(),
                    d.getDescripcion(), pcs, usuarios));
        }
        return vistas;
    }

    @Transactional
    public void crear(String nombre, String codigo, String descripcion, Long actorId, String actorCorreo) {
        String n = limpiar(nombre);
        String c = limpiar(codigo);
        validarObligatorios(n, c);
        if (departamentoRepository.existsByNombreIgnoreCase(n)) {
            throw new ReglaNegocioException("Ya existe un departamento con ese nombre.");
        }
        if (departamentoRepository.existsByCodigoIgnoreCase(c)) {
            throw new ReglaNegocioException("Ya existe un departamento con ese código.");
        }
        Departamento departamento = new Departamento(n, c, limpiarOpcional(descripcion));
        departamentoRepository.save(departamento);
        auditoriaService.cambioDepartamento(actorId, actorCorreo, n, "Alta de departamento: " + n + " (" + c + ")");
    }

    @Transactional
    public void editar(Long id, String nombre, String codigo, String descripcion, Long actorId, String actorCorreo) {
        Departamento departamento = departamentoRepository.findById(id)
                .orElseThrow(() -> new ReglaNegocioException("El departamento no existe."));
        String n = limpiar(nombre);
        String c = limpiar(codigo);
        validarObligatorios(n, c);
        if (departamentoRepository.existsByNombreIgnoreCaseAndIdNot(n, id)) {
            throw new ReglaNegocioException("Ya existe otro departamento con ese nombre.");
        }
        if (departamentoRepository.existsByCodigoIgnoreCaseAndIdNot(c, id)) {
            throw new ReglaNegocioException("Ya existe otro departamento con ese código.");
        }
        String antes = departamento.getNombre() + " (" + departamento.getCodigo() + ")";
        departamento.setNombre(n);
        departamento.setCodigo(c);
        departamento.setDescripcion(limpiarOpcional(descripcion));
        auditoriaService.cambioDepartamento(actorId, actorCorreo, n,
                "Edición de departamento: " + antes + " -> " + n + " (" + c + ")");
    }

    @Transactional
    public void eliminar(Long id, Long actorId, String actorCorreo) {
        Departamento departamento = departamentoRepository.findById(id)
                .orElseThrow(() -> new ReglaNegocioException("El departamento no existe."));
        long pcs = computadoraRepository.countByDepartamento_Id(id);
        long usuarios = usuarioRepository.countByDepartamento_Id(id);
        if (pcs > 0 || usuarios > 0) {
            throw new ReglaNegocioException(
                    "No se puede eliminar: el departamento tiene " + pcs + " PCs y " + usuarios + " usuarios.");
        }
        String nombre = departamento.getNombre();
        departamentoRepository.delete(departamento);
        auditoriaService.cambioDepartamento(actorId, actorCorreo, nombre, "Baja de departamento: " + nombre);
    }

    private static void validarObligatorios(String nombre, String codigo) {
        if (!StringUtils.hasText(nombre)) {
            throw new ReglaNegocioException("El nombre es obligatorio.");
        }
        if (!StringUtils.hasText(codigo)) {
            throw new ReglaNegocioException("El código es obligatorio.");
        }
    }

    private static String limpiar(String valor) {
        return valor == null ? null : valor.trim();
    }

    private static String limpiarOpcional(String valor) {
        String v = limpiar(valor);
        return StringUtils.hasText(v) ? v : null;
    }
}
