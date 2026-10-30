package com.agentesocial.portalrdp.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Preferencias RDP de un usuario (una por usuario). Tabla "preferencias_rdp" (V7). */
@Entity
@Table(name = "preferencias_rdp")
public class PreferenciasRdp {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_id", nullable = false, unique = true)
    private Long usuarioId;

    @Column(name = "ajuste_automatico", nullable = false)
    private boolean ajusteAutomatico = true;

    @Column(name = "pantalla_completa", nullable = false)
    private boolean pantallaCompleta = false;

    @Column(nullable = false)
    private boolean multimonitor = true;

    @Column(nullable = false)
    private int escala = 100;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SonidoRdp sonido = SonidoRdp.LOCAL;

    @Column(nullable = false)
    private boolean microfono = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AtajosRdp atajos = AtajosRdp.SOLO_PANTALLA_COMPLETA;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CalidadRdp calidad = CalidadRdp.EQUILIBRADA;

    protected PreferenciasRdp() {
        // requerido por JPA
    }

    public PreferenciasRdp(Long usuarioId) {
        this.usuarioId = usuarioId;
    }

    public Long getId() {
        return id;
    }

    public Long getUsuarioId() {
        return usuarioId;
    }

    public boolean isAjusteAutomatico() {
        return ajusteAutomatico;
    }

    public void setAjusteAutomatico(boolean ajusteAutomatico) {
        this.ajusteAutomatico = ajusteAutomatico;
    }

    public boolean isPantallaCompleta() {
        return pantallaCompleta;
    }

    public void setPantallaCompleta(boolean pantallaCompleta) {
        this.pantallaCompleta = pantallaCompleta;
    }

    public boolean isMultimonitor() {
        return multimonitor;
    }

    public void setMultimonitor(boolean multimonitor) {
        this.multimonitor = multimonitor;
    }

    public int getEscala() {
        return escala;
    }

    public void setEscala(int escala) {
        this.escala = escala;
    }

    public SonidoRdp getSonido() {
        return sonido;
    }

    public void setSonido(SonidoRdp sonido) {
        this.sonido = sonido;
    }

    public boolean isMicrofono() {
        return microfono;
    }

    public void setMicrofono(boolean microfono) {
        this.microfono = microfono;
    }

    public AtajosRdp getAtajos() {
        return atajos;
    }

    public void setAtajos(AtajosRdp atajos) {
        this.atajos = atajos;
    }

    public CalidadRdp getCalidad() {
        return calidad;
    }

    public void setCalidad(CalidadRdp calidad) {
        this.calidad = calidad;
    }
}
