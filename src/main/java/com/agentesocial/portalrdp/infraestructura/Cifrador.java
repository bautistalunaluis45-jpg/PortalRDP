package com.agentesocial.portalrdp.infraestructura;

/**
 * Frontera de cifrado simetrico (AES-GCM). Protege la contrasena de la cuenta
 * dedicada de cada PC: se guarda cifrada y la clave vive fuera de la base.
 * Ver docs/decisiones.md seccion 4.
 */
public interface Cifrador {

    /** Cifra texto plano y devuelve el criptograma (incluye el IV). */
    byte[] cifrar(String textoPlano);

    /** Descifra un criptograma producido por {@link #cifrar(String)}. */
    String descifrar(byte[] criptograma);
}
