package com.agentesocial.portalrdp.infraestructura;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Clave de cifrado (prefijo "portal.cifrado"). Se entrega por variable de entorno
 * (PORTAL_CIFRADO_CLAVE), nunca en el repositorio. 32 bytes en base64 = AES-256.
 */
@ConfigurationProperties(prefix = "portal.cifrado")
public class PropiedadesCifrado {

    private String claveBase64 = "";

    public String getClaveBase64() {
        return claveBase64;
    }

    public void setClaveBase64(String claveBase64) {
        this.claveBase64 = claveBase64;
    }
}
