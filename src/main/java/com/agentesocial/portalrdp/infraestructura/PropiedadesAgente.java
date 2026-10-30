package com.agentesocial.portalrdp.infraestructura;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Descarga del agente (prefijo "portal.agente"). {@code archivo} es la ruta del
 * ejecutable publicado del agente (.exe autocontenido) que el portal sirve para
 * instalar en las PCs destino. Se publica en Windows y se deja en esa ruta.
 */
@ConfigurationProperties(prefix = "portal.agente")
public class PropiedadesAgente {

    private String archivo = "";
    private String version = "";

    public String getArchivo() {
        return archivo;
    }

    public void setArchivo(String archivo) {
        this.archivo = archivo;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }
}
