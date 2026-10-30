package com.agentesocial.portalrdp.infraestructura;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.stereotype.Component;

/**
 * Cifrado AES-256-GCM. El criptograma es IV (12 bytes) seguido del texto cifrado
 * con su etiqueta de autenticacion (128 bits). GCM detecta manipulacion al descifrar.
 */
@Component
public class CifradorAesGcm implements Cifrador {

    private static final String TRANSFORMACION = "AES/GCM/NoPadding";
    private static final int LONGITUD_IV = 12;
    private static final int LONGITUD_ETIQUETA_BITS = 128;

    private final SecretKeySpec clave;
    private final SecureRandom aleatorio = new SecureRandom();

    public CifradorAesGcm(PropiedadesCifrado propiedades) {
        String base64 = propiedades.getClaveBase64();
        if (base64 == null || base64.isBlank()) {
            throw new IllegalStateException(
                    "Falta la clave de cifrado. Define PORTAL_CIFRADO_CLAVE (32 bytes en base64).");
        }
        byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(base64.trim());
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException("PORTAL_CIFRADO_CLAVE no es base64 valido.", ex);
        }
        if (bytes.length != 16 && bytes.length != 24 && bytes.length != 32) {
            throw new IllegalStateException(
                    "La clave de cifrado debe ser de 16, 24 o 32 bytes; recibidos: " + bytes.length);
        }
        this.clave = new SecretKeySpec(bytes, "AES");
    }

    @Override
    public byte[] cifrar(String textoPlano) {
        try {
            byte[] iv = new byte[LONGITUD_IV];
            aleatorio.nextBytes(iv);
            Cipher cipher = Cipher.getInstance(TRANSFORMACION);
            cipher.init(Cipher.ENCRYPT_MODE, clave, new GCMParameterSpec(LONGITUD_ETIQUETA_BITS, iv));
            byte[] cifrado = cipher.doFinal(textoPlano.getBytes(StandardCharsets.UTF_8));
            byte[] salida = new byte[iv.length + cifrado.length];
            System.arraycopy(iv, 0, salida, 0, iv.length);
            System.arraycopy(cifrado, 0, salida, iv.length, cifrado.length);
            return salida;
        } catch (Exception ex) {
            throw new IllegalStateException("No se pudo cifrar", ex);
        }
    }

    @Override
    public String descifrar(byte[] criptograma) {
        if (criptograma == null || criptograma.length <= LONGITUD_IV) {
            throw new IllegalArgumentException("Criptograma invalido");
        }
        try {
            byte[] iv = Arrays.copyOfRange(criptograma, 0, LONGITUD_IV);
            byte[] cifrado = Arrays.copyOfRange(criptograma, LONGITUD_IV, criptograma.length);
            Cipher cipher = Cipher.getInstance(TRANSFORMACION);
            cipher.init(Cipher.DECRYPT_MODE, clave, new GCMParameterSpec(LONGITUD_ETIQUETA_BITS, iv));
            return new String(cipher.doFinal(cifrado), StandardCharsets.UTF_8);
        } catch (Exception ex) {
            throw new IllegalStateException("No se pudo descifrar (clave incorrecta o dato manipulado)", ex);
        }
    }
}
