package com.agentesocial.portalrdp.excepcion;

/**
 * Violacion de una regla de negocio que se puede mostrar al usuario
 * (p. ej. nombre duplicado, o borrar un departamento que aun tiene PCs).
 */
public class ReglaNegocioException extends RuntimeException {

    public ReglaNegocioException(String mensaje) {
        super(mensaje);
    }
}
