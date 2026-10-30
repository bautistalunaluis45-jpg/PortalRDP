package com.agentesocial.portalrdp.dominio;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Evento de auditoria inmutable (solo lectura tras crearse). Tabla "evento_auditoria" (V3).
 * No tiene setters a proposito: se construye con el Builder y nunca se modifica.
 */
@Entity
@Table(name = "evento_auditoria")
public class EventoAuditoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private TipoEventoAuditoria tipo;

    @Column(name = "ocurrido_en", nullable = false)
    private Instant ocurridoEn;

    @Column(name = "actor_usuario_id")
    private Long actorUsuarioId;

    @Column(name = "actor_correo")
    private String actorCorreo;

    @Column(name = "usuario_afectado_id")
    private Long usuarioAfectadoId;

    @Column(name = "computadora_id")
    private Long computadoraId;

    @Column(name = "departamento_nombre")
    private String departamentoNombre;

    @Column(name = "ip_origen")
    private String ipOrigen;

    @Column
    private String detalle;

    protected EventoAuditoria() {
        // requerido por JPA
    }

    private EventoAuditoria(Builder b) {
        this.tipo = b.tipo;
        this.ocurridoEn = b.ocurridoEn;
        this.actorUsuarioId = b.actorUsuarioId;
        this.actorCorreo = b.actorCorreo;
        this.usuarioAfectadoId = b.usuarioAfectadoId;
        this.computadoraId = b.computadoraId;
        this.departamentoNombre = b.departamentoNombre;
        this.ipOrigen = b.ipOrigen;
        this.detalle = b.detalle;
    }

    public static Builder de(TipoEventoAuditoria tipo, Instant ocurridoEn) {
        return new Builder(tipo, ocurridoEn);
    }

    public Long getId() {
        return id;
    }

    public TipoEventoAuditoria getTipo() {
        return tipo;
    }

    public Instant getOcurridoEn() {
        return ocurridoEn;
    }

    public Long getActorUsuarioId() {
        return actorUsuarioId;
    }

    public String getActorCorreo() {
        return actorCorreo;
    }

    public Long getUsuarioAfectadoId() {
        return usuarioAfectadoId;
    }

    public Long getComputadoraId() {
        return computadoraId;
    }

    public String getDepartamentoNombre() {
        return departamentoNombre;
    }

    public String getIpOrigen() {
        return ipOrigen;
    }

    public String getDetalle() {
        return detalle;
    }

    /** Construye un evento. Solo {@code tipo} y {@code ocurridoEn} son obligatorios. */
    public static final class Builder {
        private final TipoEventoAuditoria tipo;
        private final Instant ocurridoEn;
        private Long actorUsuarioId;
        private String actorCorreo;
        private Long usuarioAfectadoId;
        private Long computadoraId;
        private String departamentoNombre;
        private String ipOrigen;
        private String detalle;

        private Builder(TipoEventoAuditoria tipo, Instant ocurridoEn) {
            this.tipo = tipo;
            this.ocurridoEn = ocurridoEn;
        }

        public Builder actor(Long usuarioId, String correo) {
            this.actorUsuarioId = usuarioId;
            this.actorCorreo = correo;
            return this;
        }

        public Builder usuarioAfectado(Long usuarioId) {
            this.usuarioAfectadoId = usuarioId;
            return this;
        }

        public Builder computadora(Long computadoraId) {
            this.computadoraId = computadoraId;
            return this;
        }

        public Builder departamento(String nombre) {
            this.departamentoNombre = nombre;
            return this;
        }

        public Builder ipOrigen(String ip) {
            this.ipOrigen = ip;
            return this;
        }

        public Builder detalle(String detalle) {
            this.detalle = detalle;
            return this;
        }

        public EventoAuditoria construir() {
            return new EventoAuditoria(this);
        }
    }
}
