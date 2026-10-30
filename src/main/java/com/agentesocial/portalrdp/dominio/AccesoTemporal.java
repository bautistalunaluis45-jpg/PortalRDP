package com.agentesocial.portalrdp.dominio;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Permiso que deja a un usuario ver y lanzar una PC hasta que vence o se revoca.
 * Tabla "acceso_temporal" (V2). Vigente = revocadoEn nulo y venceEn futuro.
 */
@Entity
@Table(name = "acceso_temporal")
public class AccesoTemporal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "computadora_id", nullable = false)
    private Computadora computadora;

    /** Vencimiento; nulo = acceso indefinido (no expira hasta revocarse). */
    @Column(name = "vence_en")
    private Instant venceEn;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "otorgado_por_id", nullable = false)
    private Usuario otorgadoPor;

    @Column(length = 500)
    private String motivo;

    @Column(name = "revocado_en")
    private Instant revocadoEn;

    protected AccesoTemporal() {
        // requerido por JPA
    }

    public AccesoTemporal(Usuario usuario, Computadora computadora, Instant venceEn,
                          Usuario otorgadoPor, String motivo) {
        this.usuario = usuario;
        this.computadora = computadora;
        this.venceEn = venceEn;
        this.otorgadoPor = otorgadoPor;
        this.motivo = motivo;
    }

    public Long getId() {
        return id;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public Computadora getComputadora() {
        return computadora;
    }

    public Instant getVenceEn() {
        return venceEn;
    }

    public Usuario getOtorgadoPor() {
        return otorgadoPor;
    }

    public String getMotivo() {
        return motivo;
    }

    public Instant getRevocadoEn() {
        return revocadoEn;
    }

    public void setRevocadoEn(Instant revocadoEn) {
        this.revocadoEn = revocadoEn;
    }
}
