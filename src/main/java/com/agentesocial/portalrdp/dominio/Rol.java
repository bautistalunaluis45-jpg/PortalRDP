package com.agentesocial.portalrdp.dominio;

/**
 * Rol del usuario en el portal. Determina que ve y que puede hacer.
 * Ver docs/decisiones.md seccion 3.
 */
public enum Rol {
    EMPLEADO,
    MANAGER,
    ADMIN;

    /** Etiqueta legible para la interfaz. */
    public String etiqueta() {
        return switch (this) {
            case EMPLEADO -> "Empleado";
            case MANAGER -> "Manager";
            case ADMIN -> "Administrador";
        };
    }
}
