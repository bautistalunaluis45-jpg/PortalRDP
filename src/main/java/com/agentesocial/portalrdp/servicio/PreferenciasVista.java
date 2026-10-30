package com.agentesocial.portalrdp.servicio;

import com.agentesocial.portalrdp.dominio.AtajosRdp;
import com.agentesocial.portalrdp.dominio.CalidadRdp;
import com.agentesocial.portalrdp.dominio.SonidoRdp;

/** Preferencias RDP de un usuario para la pantalla E3. */
public record PreferenciasVista(
        boolean ajusteAutomatico,
        boolean pantallaCompleta,
        boolean multimonitor,
        int escala,
        SonidoRdp sonido,
        boolean microfono,
        AtajosRdp atajos,
        CalidadRdp calidad) {
}
