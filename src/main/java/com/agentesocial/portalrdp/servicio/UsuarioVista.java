package com.agentesocial.portalrdp.servicio;

import com.agentesocial.portalrdp.dominio.Rol;

/** Datos de un usuario para las pantallas de administracion (lista y detalle). */
public record UsuarioVista(
        Long id,
        String nombre,
        String correo,
        Long departamentoId,
        String departamentoNombre,
        Rol rol,
        boolean activo) {

    /** Iniciales para el avatar (1 o 2 letras). */
    public String iniciales() {
        if (nombre == null || nombre.isBlank()) {
            return "?";
        }
        String[] partes = nombre.trim().split("\\s+");
        String s = partes[0].substring(0, 1);
        if (partes.length > 1) {
            s += partes[partes.length - 1].substring(0, 1);
        }
        return s.toUpperCase();
    }

    public String rolEtiqueta() {
        return rol.etiqueta();
    }
}
