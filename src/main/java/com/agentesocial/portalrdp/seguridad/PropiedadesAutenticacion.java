package com.agentesocial.portalrdp.seguridad;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuracion de autenticacion del portal (prefijo "portal.auth").
 *
 * El filtro principal de acceso es el alta previa del usuario (solo entra quien
 * el admin registro). La restriccion por dominio es OPCIONAL y sirve como defensa
 * adicional: si no se configura ningun dominio, se acepta cualquier dominio y
 * manda el alta previa.
 */
@ConfigurationProperties(prefix = "portal.auth")
public class PropiedadesAutenticacion {

    /**
     * Dominios de Workspace aceptados (p. ej. healthcarepi.com). Lista VACIA = sin
     * restriccion de dominio (el unico filtro es el alta previa del usuario).
     */
    private List<String> dominiosPermitidos = List.of();

    public List<String> getDominiosPermitidos() {
        return dominiosPermitidos;
    }

    public void setDominiosPermitidos(List<String> dominiosPermitidos) {
        this.dominiosPermitidos = dominiosPermitidos;
    }

    /** Dominios configurados, ya normalizados (sin espacios ni vacios, en minusculas). */
    private List<String> dominiosNormalizados() {
        return dominiosPermitidos.stream()
                .map(d -> d == null ? "" : d.trim().toLowerCase())
                .filter(d -> !d.isEmpty())
                .toList();
    }

    /**
     * Indica si el correo es aceptable por dominio. Si no hay dominios configurados,
     * acepta cualquiera (el filtro real es el alta previa). Si hay lista, el dominio
     * del correo debe estar en ella (sin distinguir mayusculas).
     */
    public boolean dominioPermitido(String correo) {
        List<String> dominios = dominiosNormalizados();
        if (dominios.isEmpty()) {
            return true;
        }
        if (correo == null) {
            return false;
        }
        int arroba = correo.lastIndexOf('@');
        if (arroba < 0 || arroba == correo.length() - 1) {
            return false;
        }
        String dominio = correo.substring(arroba + 1).toLowerCase();
        return dominios.contains(dominio);
    }
}
