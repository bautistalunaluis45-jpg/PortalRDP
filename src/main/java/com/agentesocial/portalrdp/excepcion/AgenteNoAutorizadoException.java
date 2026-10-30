package com.agentesocial.portalrdp.excepcion;

/** El token del agente es inválido o no corresponde a ninguna PC. Se traduce a HTTP 401. */
public class AgenteNoAutorizadoException extends RuntimeException {

    public AgenteNoAutorizadoException(String mensaje) {
        super(mensaje);
    }
}
