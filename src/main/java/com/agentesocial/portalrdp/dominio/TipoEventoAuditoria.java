package com.agentesocial.portalrdp.dominio;

/**
 * Tipos de evento de auditoria. Ver docs/decisiones.md seccion 17.
 * Se agregan a medida que cada accion se implementa.
 */
public enum TipoEventoAuditoria {
    ENTRADA_PORTAL,
    SALIDA_PORTAL,
    LANZAMIENTO_RDP,
    FIN_SESION,
    EXPULSION,
    DESCONECTAR,
    SHADOW,
    ACCESO_TEMPORAL_CREADO,
    ACCESO_TEMPORAL_REVOCADO,
    ACCESO_TEMPORAL_VENCIDO,
    CAMBIO_USUARIO,
    CAMBIO_COMPUTADORA,
    CAMBIO_DEPARTAMENTO,
    CAMBIO_PASSWORD_PC;

    /** Etiqueta legible para la interfaz. */
    public String etiqueta() {
        return switch (this) {
            case ENTRADA_PORTAL -> "Entrada al portal";
            case SALIDA_PORTAL -> "Salida del portal";
            case LANZAMIENTO_RDP -> "Lanzamiento RDP";
            case FIN_SESION -> "Fin de sesión";
            case EXPULSION -> "Expulsión por conexión";
            case DESCONECTAR -> "Desconexión";
            case SHADOW -> "Shadow";
            case ACCESO_TEMPORAL_CREADO -> "Acceso temporal creado";
            case ACCESO_TEMPORAL_REVOCADO -> "Acceso temporal revocado";
            case ACCESO_TEMPORAL_VENCIDO -> "Acceso temporal vencido";
            case CAMBIO_USUARIO -> "Cambio de usuario";
            case CAMBIO_COMPUTADORA -> "Cambio de computadora";
            case CAMBIO_DEPARTAMENTO -> "Cambio de departamento";
            case CAMBIO_PASSWORD_PC -> "Cambio de contraseña de PC";
        };
    }
}
