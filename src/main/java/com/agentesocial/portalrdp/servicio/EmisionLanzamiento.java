package com.agentesocial.portalrdp.servicio;

import com.agentesocial.portalrdp.dominio.TipoLanzamiento;

/**
 * Resultado de emitir un lanzamiento. Si {@code exito} es false, {@code mensaje} dice
 * por qué se rechazó (ya quedó auditado). Si es true, trae el token y la URL del protocolo
 * que el navegador debe abrir para que el helper lo canjee.
 */
public record EmisionLanzamiento(
        boolean exito,
        String mensaje,
        String token,
        String protocolo,
        String urlProtocolo,
        TipoLanzamiento tipo,
        String computadoraNombre,
        int vigenciaSegundos) {

    public static EmisionLanzamiento rechazo(String mensaje) {
        return new EmisionLanzamiento(false, mensaje, null, null, null, null, null, 0);
    }

    public static EmisionLanzamiento ok(String token, String protocolo, String urlProtocolo,
                                        TipoLanzamiento tipo, String computadoraNombre, int vigenciaSegundos) {
        return new EmisionLanzamiento(true, null, token, protocolo, urlProtocolo, tipo, computadoraNombre, vigenciaSegundos);
    }
}
