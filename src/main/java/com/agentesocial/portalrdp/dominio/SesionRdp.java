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

/** Sesión de Windows de una PC, reportada por el agente. Tabla "sesion_rdp" (V8). */
@Entity
@Table(name = "sesion_rdp")
public class SesionRdp {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "computadora_id", nullable = false)
    private Computadora computadora;

    @Column(name = "id_sesion_windows", nullable = false, length = 50)
    private String idSesionWindows;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoSesion estado;

    @Column(name = "usuario_windows", length = 200)
    private String usuarioWindows;

    @Column(name = "ip_origen", length = 64)
    private String ipOrigen;

    @Column(nullable = false)
    private Instant desde;

    @Column(name = "idle_segundos", nullable = false)
    private int idleSegundos;

    @Column(name = "fin_en")
    private Instant finEn;

    /** Usuario del portal atribuido (fase 5, por IP del lanzamiento). Nulo por ahora. */
    @Column(name = "usuario_atribuido_id")
    private Long usuarioAtribuidoId;

    protected SesionRdp() {
        // requerido por JPA
    }

    public SesionRdp(Computadora computadora, String idSesionWindows, EstadoSesion estado, Instant desde) {
        this.computadora = computadora;
        this.idSesionWindows = idSesionWindows;
        this.estado = estado;
        this.desde = desde;
    }

    public Long getId() {
        return id;
    }

    public Computadora getComputadora() {
        return computadora;
    }

    public String getIdSesionWindows() {
        return idSesionWindows;
    }

    public EstadoSesion getEstado() {
        return estado;
    }

    public void setEstado(EstadoSesion estado) {
        this.estado = estado;
    }

    public String getUsuarioWindows() {
        return usuarioWindows;
    }

    public void setUsuarioWindows(String usuarioWindows) {
        this.usuarioWindows = usuarioWindows;
    }

    public String getIpOrigen() {
        return ipOrigen;
    }

    public void setIpOrigen(String ipOrigen) {
        this.ipOrigen = ipOrigen;
    }

    public Instant getDesde() {
        return desde;
    }

    public int getIdleSegundos() {
        return idleSegundos;
    }

    public void setIdleSegundos(int idleSegundos) {
        this.idleSegundos = idleSegundos;
    }

    public Instant getFinEn() {
        return finEn;
    }

    public void setFinEn(Instant finEn) {
        this.finEn = finEn;
    }

    public Long getUsuarioAtribuidoId() {
        return usuarioAtribuidoId;
    }

    public void setUsuarioAtribuidoId(Long usuarioAtribuidoId) {
        this.usuarioAtribuidoId = usuarioAtribuidoId;
    }
}
