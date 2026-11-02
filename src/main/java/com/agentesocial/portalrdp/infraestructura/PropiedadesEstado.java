package com.agentesocial.portalrdp.infraestructura;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Parametros del calculo de estado de las computadoras (prefijo "portal.estado").
 * Una PC se considera SIN_CONEXION si no reporta latido dentro de este umbral.
 * Valor propuesto en docs/decisiones.md seccion 15: 90 s (configurable).
 */
@ConfigurationProperties(prefix = "portal.estado")
public class PropiedadesEstado {

    private long sinConexionTrasSegundos = 90;

    /** Inactividad (segundos) a partir de la cual una sesión se marca "idle" en el resumen. */
    private long idleTrasSegundos = 600;

    public long getSinConexionTrasSegundos() {
        return sinConexionTrasSegundos;
    }

    public void setSinConexionTrasSegundos(long sinConexionTrasSegundos) {
        this.sinConexionTrasSegundos = sinConexionTrasSegundos;
    }

    public Duration umbralSinConexion() {
        return Duration.ofSeconds(sinConexionTrasSegundos);
    }

    public long getIdleTrasSegundos() {
        return idleTrasSegundos;
    }

    public void setIdleTrasSegundos(long idleTrasSegundos) {
        this.idleTrasSegundos = idleTrasSegundos;
    }
}
