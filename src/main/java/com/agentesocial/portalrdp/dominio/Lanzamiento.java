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
 * Token de lanzamiento de un solo uso (fase 5). El portal lo emite cuando alguien
 * pulsa Conectar o Shadow; el helper lo canjea una vez para recibir host, cuenta,
 * contraseña y preferencias. Solo se guarda el hash del token. Tabla "lanzamiento" (V9).
 * Ver docs/decisiones.md 5, 12.
 */
@Entity
@Table(name = "lanzamiento")
public class Lanzamiento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "computadora_id", nullable = false)
    private Computadora computadora;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoLanzamiento tipo;

    @Column(name = "token_hash", nullable = false, length = 64, unique = true)
    private String tokenHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoLanzamiento estado;

    @Column(name = "creado_en", nullable = false)
    private Instant creadoEn;

    @Column(name = "vence_en", nullable = false)
    private Instant venceEn;

    @Column(name = "canjeado_en")
    private Instant canjeadoEn;

    @Column(name = "ip_helper", length = 64)
    private String ipHelper;

    /** Para SHADOW: id de la sesión de Windows a observar. Nulo en CONECTAR. */
    @Column(name = "id_sesion_objetivo", length = 50)
    private String idSesionObjetivo;

    protected Lanzamiento() {
        // requerido por JPA
    }

    public Lanzamiento(Usuario usuario, Computadora computadora, TipoLanzamiento tipo, String tokenHash,
                       Instant creadoEn, Instant venceEn, String idSesionObjetivo) {
        this.usuario = usuario;
        this.computadora = computadora;
        this.tipo = tipo;
        this.tokenHash = tokenHash;
        this.estado = EstadoLanzamiento.EMITIDO;
        this.creadoEn = creadoEn;
        this.venceEn = venceEn;
        this.idSesionObjetivo = idSesionObjetivo;
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

    public TipoLanzamiento getTipo() {
        return tipo;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public EstadoLanzamiento getEstado() {
        return estado;
    }

    public void setEstado(EstadoLanzamiento estado) {
        this.estado = estado;
    }

    public Instant getCreadoEn() {
        return creadoEn;
    }

    public Instant getVenceEn() {
        return venceEn;
    }

    public Instant getCanjeadoEn() {
        return canjeadoEn;
    }

    public void setCanjeadoEn(Instant canjeadoEn) {
        this.canjeadoEn = canjeadoEn;
    }

    public String getIpHelper() {
        return ipHelper;
    }

    public void setIpHelper(String ipHelper) {
        this.ipHelper = ipHelper;
    }

    public String getIdSesionObjetivo() {
        return idSesionObjetivo;
    }
}
