package com.agentesocial.portalrdp.infraestructura;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Parámetros del lanzamiento (prefijo "portal.lanzamiento"). {@code vigenciaSegundos}
 * es la vida del token de un solo uso (por defecto 60 s, docs/decisiones.md 15).
 * {@code protocolo} es el esquema URL que registra el helper (provisional: "pulsedesk").
 */
@ConfigurationProperties(prefix = "portal.lanzamiento")
public class PropiedadesLanzamiento {

    private int vigenciaSegundos = 60;
    private String protocolo = "pulsedesk";

    public int getVigenciaSegundos() {
        return vigenciaSegundos;
    }

    public void setVigenciaSegundos(int vigenciaSegundos) {
        this.vigenciaSegundos = vigenciaSegundos;
    }

    public Duration vigencia() {
        return Duration.ofSeconds(Math.max(5, vigenciaSegundos));
    }

    public String getProtocolo() {
        return protocolo;
    }

    public void setProtocolo(String protocolo) {
        this.protocolo = protocolo;
    }
}
