package com.agentesocial.portalrdp.servicio;

import java.util.List;

/**
 * Datos "en vivo" del panel Resumen (manager/admin). Solo estado actual (sin histórico).
 */
public record ResumenVista(
        int totalPcs,
        int libres,
        int ocupadas,
        int sinConexion,
        List<SesionEnVivo> enUso,
        int idleCount,
        List<PcAtencion> sinConexionPcs,
        List<PcAtencion> sinCredencialPcs,
        int accesosVigentes,
        List<AccesoPorVencer> porVencer) {

    /** Una sesión conectada ahora mismo. */
    public record SesionEnVivo(String computadora, String departamento, String usuario,
                               String desdeTexto, String idleTexto, boolean idle) {
    }

    /** Una PC que requiere atención (sin conexión o sin credencial). */
    public record PcAtencion(String nombre, String departamento, String detalle) {
    }

    /** Un acceso temporal próximo a vencer. */
    public record AccesoPorVencer(String usuario, String computadora, String venceTexto) {
    }
}
