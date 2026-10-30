package com.agentesocial.portalrdp.seguridad;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PropiedadesAutenticacionTest {

    private PropiedadesAutenticacion propiedades;

    @BeforeEach
    void preparar() {
        propiedades = new PropiedadesAutenticacion();
        propiedades.setDominiosPermitidos(List.of("healthcarepi.com"));
    }

    @Test
    void aceptaCorreoDelDominioPermitido() {
        assertThat(propiedades.dominioPermitido("ana@healthcarepi.com")).isTrue();
    }

    @Test
    void ignoraMayusculas() {
        assertThat(propiedades.dominioPermitido("Ana@HealthCarePI.com")).isTrue();
    }

    @Test
    void rechazaOtroDominio() {
        assertThat(propiedades.dominioPermitido("ana@gmail.com")).isFalse();
    }

    @Test
    void rechazaCorreoInvalidoONulo() {
        assertThat(propiedades.dominioPermitido(null)).isFalse();
        assertThat(propiedades.dominioPermitido("sin-arroba")).isFalse();
        assertThat(propiedades.dominioPermitido("termina-en-arroba@")).isFalse();
    }

    @Test
    void sinDominiosConfiguradosAceptaCualquierDominio() {
        // Lista vacia = sin restriccion de dominio; el filtro real es el alta previa.
        propiedades.setDominiosPermitidos(List.of());
        assertThat(propiedades.dominioPermitido("ana@gmail.com")).isTrue();
        assertThat(propiedades.dominioPermitido("ana@healthcarepi.com")).isTrue();
    }

    @Test
    void entradasVaciasOEnBlancoCuentanComoSinRestriccion() {
        propiedades.setDominiosPermitidos(List.of("   ", ""));
        assertThat(propiedades.dominioPermitido("ana@gmail.com")).isTrue();
    }
}
