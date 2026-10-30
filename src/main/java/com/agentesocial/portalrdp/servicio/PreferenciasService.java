package com.agentesocial.portalrdp.servicio;

import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.agentesocial.portalrdp.dominio.AtajosRdp;
import com.agentesocial.portalrdp.dominio.CalidadRdp;
import com.agentesocial.portalrdp.dominio.PreferenciasRdp;
import com.agentesocial.portalrdp.dominio.SonidoRdp;
import com.agentesocial.portalrdp.excepcion.ReglaNegocioException;
import com.agentesocial.portalrdp.repositorio.PreferenciasRdpRepository;

/** Preferencias RDP por usuario (E3). Personales: no se auditan. */
@Service
public class PreferenciasService {

    private static final Set<Integer> ESCALAS = Set.of(100, 125, 150, 175, 200);

    private final PreferenciasRdpRepository repositorio;

    public PreferenciasService(PreferenciasRdpRepository repositorio) {
        this.repositorio = repositorio;
    }

    /** Devuelve las preferencias del usuario, o los valores por defecto si aún no las guardó. */
    @Transactional(readOnly = true)
    public PreferenciasVista obtener(Long usuarioId) {
        return repositorio.findByUsuarioId(usuarioId)
                .map(PreferenciasService::aVista)
                .orElseGet(() -> aVista(new PreferenciasRdp(usuarioId)));
    }

    @Transactional
    public void guardar(Long usuarioId, boolean ajusteAutomatico, boolean pantallaCompleta, boolean multimonitor,
                        int escala, SonidoRdp sonido, boolean microfono, AtajosRdp atajos, CalidadRdp calidad) {
        if (!ESCALAS.contains(escala)) {
            throw new ReglaNegocioException("Escala no válida.");
        }
        if (sonido == null || atajos == null || calidad == null) {
            throw new ReglaNegocioException("Faltan opciones obligatorias.");
        }
        PreferenciasRdp p = repositorio.findByUsuarioId(usuarioId).orElseGet(() -> new PreferenciasRdp(usuarioId));
        p.setAjusteAutomatico(ajusteAutomatico);
        p.setPantallaCompleta(pantallaCompleta);
        p.setMultimonitor(multimonitor);
        p.setEscala(escala);
        p.setSonido(sonido);
        p.setMicrofono(microfono);
        p.setAtajos(atajos);
        p.setCalidad(calidad);
        repositorio.save(p);
    }

    private static PreferenciasVista aVista(PreferenciasRdp p) {
        return new PreferenciasVista(p.isAjusteAutomatico(), p.isPantallaCompleta(), p.isMultimonitor(),
                p.getEscala(), p.getSonido(), p.isMicrofono(), p.getAtajos(), p.getCalidad());
    }
}
