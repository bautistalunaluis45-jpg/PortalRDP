package com.agentesocial.portalrdp.dominio;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Orden del portal hacia el agente de una PC (canal de órdenes, fase 5 / §7).
 * El agente la recoge en su latido, la ejecuta (lista blanca) y reporta el resultado.
 * Tabla "orden_agente" (V10).
 */
@Entity
@Table(name = "orden_agente")
public class OrdenAgente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "computadora_id", nullable = false)
    private Computadora computadora;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoOrden tipo;

    /** Sesión de Windows objetivo (el agente verifica que siga coincidiendo antes de actuar). */
    @Column(name = "id_sesion_objetivo", nullable = false, length = 50)
    private String idSesionObjetivo;

    /** Usuario de Windows que se espera desconectar (para la auditoría y la verificación). */
    @Column(name = "usuario_windows", length = 200)
    private String usuarioWindows;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoOrden estado;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "creada_por_id", nullable = false)
    private Usuario creadaPor;

    @Column(name = "creada_en", nullable = false)
    private Instant creadaEn;

    @Column(name = "entregada_en")
    private Instant entregadaEn;

    @Column(name = "resuelta_en")
    private Instant resueltaEn;

    @Column(length = 500)
    private String detalle;

    protected OrdenAgente() {
        // requerido por JPA
    }

    public OrdenAgente(Computadora computadora, TipoOrden tipo, String idSesionObjetivo, String usuarioWindows,
                       Usuario creadaPor, Instant creadaEn) {
        this.computadora = computadora;
        this.tipo = tipo;
        this.idSesionObjetivo = idSesionObjetivo;
        this.usuarioWindows = usuarioWindows;
        this.creadaPor = creadaPor;
        this.creadaEn = creadaEn;
        this.estado = EstadoOrden.PENDIENTE;
    }

    public Long getId() {
        return id;
    }

    public Computadora getComputadora() {
        return computadora;
    }

    public TipoOrden getTipo() {
        return tipo;
    }

    public String getIdSesionObjetivo() {
        return idSesionObjetivo;
    }

    public String getUsuarioWindows() {
        return usuarioWindows;
    }

    public EstadoOrden getEstado() {
        return estado;
    }

    public void setEstado(EstadoOrden estado) {
        this.estado = estado;
    }

    public Usuario getCreadaPor() {
        return creadaPor;
    }

    public Instant getCreadaEn() {
        return creadaEn;
    }

    public Instant getEntregadaEn() {
        return entregadaEn;
    }

    public void setEntregadaEn(Instant entregadaEn) {
        this.entregadaEn = entregadaEn;
    }

    public Instant getResueltaEn() {
        return resueltaEn;
    }

    public void setResueltaEn(Instant resueltaEn) {
        this.resueltaEn = resueltaEn;
    }

    public String getDetalle() {
        return detalle;
    }

    public void setDetalle(String detalle) {
        this.detalle = detalle;
    }
}
