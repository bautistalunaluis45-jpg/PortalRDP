package com.agentesocial.portalrdp.infraestructura;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class CifradorAesGcmTest {

    private static final String CLAVE = "N5zFQ2LpwftSisk7iMR2rLhYaQgVUmfxb+hcd3NROMI=";

    private CifradorAesGcm crear(String claveBase64) {
        PropiedadesCifrado p = new PropiedadesCifrado();
        p.setClaveBase64(claveBase64);
        return new CifradorAesGcm(p);
    }

    @Test
    void cifraYDescifraElMismoTexto() {
        CifradorAesGcm cifrador = crear(CLAVE);
        String secreto = "Contrasena-super-larga-123!";
        assertThat(cifrador.descifrar(cifrador.cifrar(secreto))).isEqualTo(secreto);
    }

    @Test
    void producenCriptogramasDistintosPorElIvAleatorio() {
        CifradorAesGcm cifrador = crear(CLAVE);
        assertThat(cifrador.cifrar("misma")).isNotEqualTo(cifrador.cifrar("misma"));
    }

    @Test
    void detectaManipulacionDelCriptograma() {
        CifradorAesGcm cifrador = crear(CLAVE);
        byte[] cripto = cifrador.cifrar("dato");
        cripto[cripto.length - 1] ^= 0x01; // altera un byte
        assertThatThrownBy(() -> cifrador.descifrar(cripto)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void otraClaveNoPuedeDescifrar() {
        byte[] cripto = crear(CLAVE).cifrar("dato");
        CifradorAesGcm otro = crear("2WpnwtH5vuMfVuMNV9qRS6VNv2OvQmGZFwU9JyX3yKY=");
        assertThatThrownBy(() -> otro.descifrar(cripto)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void rechazaClaveVaciaOInvalida() {
        assertThatThrownBy(() -> crear("")).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> crear("Y29ydGE=")).isInstanceOf(IllegalStateException.class); // "corta"
    }
}
