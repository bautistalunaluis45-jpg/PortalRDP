package com.agentesocial.portalrdp.infraestructura;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Descarga del helper (prefijo "portal.helper"). {@code archivo} es la ruta del
 * ejecutable publicado del helper (.exe) que el portal sirve para instalar en la
 * PC del operador (instalación por usuario, sin admin). Ver docs/decisiones.md 19.
 */
@ConfigurationProperties(prefix = "portal.helper")
public class PropiedadesHelper {

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
