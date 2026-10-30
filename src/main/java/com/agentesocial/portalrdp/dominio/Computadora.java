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
 * PC destino a la que los empleados se conectan por RDP. Tabla "computadora" (V1).
 * El estado (LIBRE/OCUPADA/SIN_CONEXION) no se guarda: se calcula.
 */
@Entity
@Table(name = "computadora")
public class Computadora {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String nombre;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "departamento_id", nullable = false)
    private Departamento departamento;

    @Column(name = "direccion_tailscale", nullable = false, unique = true)
    private String direccionTailscale;

    /** Usuario de la cuenta local dedicada de la PC (no secreto). */
    @Column(name = "cuenta_usuario", length = 100)
    private String cuentaUsuario;

    /** Contrasena de la cuenta dedicada, cifrada (AES-GCM). Nula hasta que el admin la captura. */
    @Column(name = "credencial_cifrada")
    private byte[] credencialCifrada;

    /** Hash del token por PC del agente; nulo hasta que el agente se da de alta. */
    @Column(name = "token_agente_hash", unique = true)
    private String tokenAgenteHash;

    @Column(name = "ultimo_latido")
    private Instant ultimoLatido;

    @Column(nullable = false)
    private boolean activa = true;

    protected Computadora() {
        // requerido por JPA
    }

    public Computadora(String nombre, Departamento departamento, String direccionTailscale) {
        this.nombre = nombre;
        this.departamento = departamento;
        this.direccionTailscale = direccionTailscale;
        this.activa = true;
    }

    public Long getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public Departamento getDepartamento() {
        return departamento;
    }

    public void setDepartamento(Departamento departamento) {
        this.departamento = departamento;
    }

    public String getDireccionTailscale() {
        return direccionTailscale;
    }

    public void setDireccionTailscale(String direccionTailscale) {
        this.direccionTailscale = direccionTailscale;
    }

    public String getCuentaUsuario() {
        return cuentaUsuario;
    }

    public void setCuentaUsuario(String cuentaUsuario) {
        this.cuentaUsuario = cuentaUsuario;
    }

    public byte[] getCredencialCifrada() {
        return credencialCifrada;
    }

    public void setCredencialCifrada(byte[] credencialCifrada) {
        this.credencialCifrada = credencialCifrada;
    }

    public String getTokenAgenteHash() {
        return tokenAgenteHash;
    }

    public void setTokenAgenteHash(String tokenAgenteHash) {
        this.tokenAgenteHash = tokenAgenteHash;
    }

    public Instant getUltimoLatido() {
        return ultimoLatido;
    }

    public void setUltimoLatido(Instant ultimoLatido) {
        this.ultimoLatido = ultimoLatido;
    }

    public boolean isActiva() {
        return activa;
    }

    public void setActiva(boolean activa) {
        this.activa = activa;
    }
}
