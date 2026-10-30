package com.agentesocial.portalrdp.infraestructura;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

/** Generación y hash de tokens opacos (token por PC del agente, y futuros tokens de lanzamiento). */
public final class Tokens {

    private static final SecureRandom ALEATORIO = new SecureRandom();

    private Tokens() {
    }

    /** Token aleatorio de 256 bits en base64url sin relleno. */
    public static String generar() {
        byte[] bytes = new byte[32];
        ALEATORIO.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /** Hash SHA-256 (hex) del token. Solo se guarda el hash; el token en claro no se almacena. */
    public static String hash(String token) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] h = md.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(h);
        } catch (Exception ex) {
            throw new IllegalStateException("No se pudo calcular el hash del token", ex);
        }
    }
}
