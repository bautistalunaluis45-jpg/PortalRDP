package com.agentesocial.portalrdp.excepcion;

/**
 * Se lanza cuando una cuenta autenticada por Google no corresponde a un usuario
 * activo del portal. El alta de usuarios la hace el admin (caso A1); Google solo
 * autentica, no da de alta.
 */
public class AccesoDenegadoException extends RuntimeException {

    public AccesoDenegadoException(String mensaje) {
        super(mensaje);
    }
}
