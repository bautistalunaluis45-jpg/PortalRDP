package com.agentesocial.portalrdp.servicio;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.agentesocial.portalrdp.dominio.Computadora;
import com.agentesocial.portalrdp.dominio.EventoAuditoria;
import com.agentesocial.portalrdp.dominio.TipoEventoAuditoria;
import com.agentesocial.portalrdp.repositorio.ComputadoraRepository;
import com.agentesocial.portalrdp.repositorio.EventoAuditoriaRepository;

import jakarta.persistence.criteria.Predicate;

/** Consulta de la bitácora de auditoría (A10): filtros, paginación, resumen y exportación CSV. */
@Service
public class ConsultaAuditoriaService {

    public static final int TAMANO_PAGINA = 25;
    private static final int TOPE_CSV = 10_000;
    private static final DateTimeFormatter FORMATO =
            DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm:ss", new Locale("es"));

    private final EventoAuditoriaRepository repositorio;
    private final ComputadoraRepository computadoraRepository;
    private final Clock reloj;

    public ConsultaAuditoriaService(EventoAuditoriaRepository repositorio,
                                    ComputadoraRepository computadoraRepository,
                                    Clock reloj) {
        this.repositorio = repositorio;
        this.computadoraRepository = computadoraRepository;
        this.reloj = reloj;
    }

    @Transactional(readOnly = true)
    public ResultadoAuditoria consultar(FiltroAuditoria filtro, int pagina) {
        int p = Math.max(pagina, 0);
        Pageable pageable = PageRequest.of(p, TAMANO_PAGINA, Sort.by(Sort.Direction.DESC, "ocurridoEn"));
        Page<EventoAuditoria> page = repositorio.findAll(spec(filtro), pageable);
        List<EventoAuditoriaFilaVista> filas = mapear(page.getContent());
        return new ResultadoAuditoria(filas, p, page.getTotalPages(), page.getTotalElements());
    }

    @Transactional(readOnly = true)
    public ResumenAuditoria resumen() {
        Instant inicioHoy = LocalDate.now(reloj).atStartOfDay(reloj.getZone()).toInstant();
        return new ResumenAuditoria(
                repositorio.count(),
                repositorio.countByOcurridoEnGreaterThanEqual(inicioHoy),
                repositorio.countByTipo(TipoEventoAuditoria.ENTRADA_PORTAL),
                repositorio.countByTipo(TipoEventoAuditoria.ACCESO_TEMPORAL_REVOCADO));
    }

    @Transactional(readOnly = true)
    public String exportarCsv(FiltroAuditoria filtro) {
        Pageable tope = PageRequest.of(0, TOPE_CSV, Sort.by(Sort.Direction.DESC, "ocurridoEn"));
        List<EventoAuditoriaFilaVista> filas = mapear(
                repositorio.findAll(spec(filtro), tope).getContent());
        StringBuilder sb = new StringBuilder("Fecha,Actor,Computadora,Evento,Detalle,IP\n");
        for (EventoAuditoriaFilaVista f : filas) {
            sb.append(csv(f.fecha())).append(',').append(csv(f.actor())).append(',')
                    .append(csv(f.computadora())).append(',').append(csv(f.tipoEtiqueta())).append(',')
                    .append(csv(f.detalle())).append(',').append(csv(f.ip())).append('\n');
        }
        return sb.toString();
    }

    private List<EventoAuditoriaFilaVista> mapear(List<EventoAuditoria> eventos) {
        List<Long> ids = eventos.stream().map(EventoAuditoria::getComputadoraId)
                .filter(x -> x != null).distinct().toList();
        Map<Long, String> nombres = ids.isEmpty() ? Map.of()
                : computadoraRepository.findAllById(ids).stream()
                        .collect(Collectors.toMap(Computadora::getId, Computadora::getNombre));

        List<EventoAuditoriaFilaVista> filas = new ArrayList<>();
        for (EventoAuditoria e : eventos) {
            String comp = e.getComputadoraId() == null ? "—"
                    : nombres.getOrDefault(e.getComputadoraId(), "#" + e.getComputadoraId());
            filas.add(new EventoAuditoriaFilaVista(
                    FORMATO.format(e.getOcurridoEn().atZone(reloj.getZone())),
                    e.getActorCorreo() == null ? "Sistema" : e.getActorCorreo(),
                    comp,
                    e.getTipo().etiqueta(),
                    color(e.getTipo()),
                    e.getDetalle() == null ? "" : e.getDetalle(),
                    e.getIpOrigen() == null ? "—" : e.getIpOrigen()));
        }
        return filas;
    }

    private Specification<EventoAuditoria> spec(FiltroAuditoria f) {
        TipoEventoAuditoria tipo = parseTipo(f.tipo());
        Instant desde;
        Instant hastaExclusive = null;
        if ("custom".equals(f.rango())) {
            desde = f.desde() == null ? null : f.desde().atStartOfDay(reloj.getZone()).toInstant();
            // "hasta" es inclusiva por día: usamos el inicio del día siguiente como límite exclusivo.
            hastaExclusive = f.hasta() == null ? null : f.hasta().plusDays(1).atStartOfDay(reloj.getZone()).toInstant();
        } else {
            desde = desdePorRango(f.rango(), reloj.instant());
        }
        final Instant desdeFinal = desde;
        final Instant hastaFinal = hastaExclusive;
        return (root, cq, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            if (StringUtils.hasText(f.q())) {
                ps.add(cb.like(cb.lower(root.get("actorCorreo")), "%" + f.q().trim().toLowerCase() + "%"));
            }
            if (f.computadoraId() != null) {
                ps.add(cb.equal(root.get("computadoraId"), f.computadoraId()));
            }
            if (tipo != null) {
                ps.add(cb.equal(root.get("tipo"), tipo));
            }
            if (desdeFinal != null) {
                ps.add(cb.greaterThanOrEqualTo(root.get("ocurridoEn"), desdeFinal));
            }
            if (hastaFinal != null) {
                ps.add(cb.lessThan(root.get("ocurridoEn"), hastaFinal));
            }
            return cb.and(ps.toArray(new Predicate[0]));
        };
    }

    static Instant desdePorRango(String rango, Instant ahora) {
        if (rango == null) {
            return ahora.minus(Duration.ofDays(7));
        }
        return switch (rango) {
            case "24h" -> ahora.minus(Duration.ofHours(24));
            case "30d" -> ahora.minus(Duration.ofDays(30));
            case "todo" -> null;
            default -> ahora.minus(Duration.ofDays(7));
        };
    }

    private static TipoEventoAuditoria parseTipo(String tipoStr) {
        if (!StringUtils.hasText(tipoStr)) {
            return null;
        }
        try {
            return TipoEventoAuditoria.valueOf(tipoStr.trim());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static String color(TipoEventoAuditoria tipo) {
        return switch (tipo) {
            case ENTRADA_PORTAL, LANZAMIENTO_RDP -> "primary";
            case ACCESO_TEMPORAL_CREADO -> "tertiary";
            case EXPULSION, DESCONECTAR, ACCESO_TEMPORAL_REVOCADO -> "error";
            case SALIDA_PORTAL, FIN_SESION, SHADOW -> "secondary";
            default -> "outline";
        };
    }

    private static String csv(String valor) {
        String v = valor == null ? "" : valor;
        if (v.contains(",") || v.contains("\"") || v.contains("\n")) {
            return '"' + v.replace("\"", "\"\"") + '"';
        }
        return v;
    }

    /** Expuesto para la UI: etiquetas de tipos de evento (valor enum -> etiqueta). */
    public List<Map.Entry<String, String>> tiposDisponibles() {
        Function<TipoEventoAuditoria, Map.Entry<String, String>> aEntrada =
                t -> Map.entry(t.name(), t.etiqueta());
        List<Map.Entry<String, String>> lista = new ArrayList<>();
        for (TipoEventoAuditoria t : TipoEventoAuditoria.values()) {
            lista.add(aEntrada.apply(t));
        }
        return lista;
    }
}
