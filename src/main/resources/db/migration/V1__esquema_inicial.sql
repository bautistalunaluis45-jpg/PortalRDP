-- Portal RDP - esquema inicial (fase 1).
-- Tablas base: departamento, usuario, computadora.
-- Las demas entidades del modelo (acceso_temporal, preferencias_rdp, regla_rdp,
-- lanzamiento, sesion_rdp, evento_auditoria) llegan en fases posteriores,
-- segun docs/decisiones.md seccion 12.

create table departamento (
    id     bigint generated always as identity primary key,
    nombre varchar(120) not null,
    constraint uq_departamento_nombre unique (nombre)
);

create table usuario (
    id              bigint generated always as identity primary key,
    nombre          varchar(160) not null,
    -- El correo se guarda en minusculas (regla aplicada en la capa de servicio).
    correo          varchar(254) not null,
    -- Identificador estable de Google (claim "sub"); nulo hasta el primer login.
    google_sub      varchar(255),
    departamento_id bigint       not null,
    rol             varchar(20)  not null,
    activo          boolean      not null default true,
    constraint uq_usuario_correo unique (correo),
    constraint uq_usuario_google_sub unique (google_sub),
    constraint fk_usuario_departamento foreign key (departamento_id) references departamento (id),
    constraint ck_usuario_rol check (rol in ('EMPLEADO', 'MANAGER', 'ADMIN'))
);

create index ix_usuario_departamento on usuario (departamento_id);

create table computadora (
    id                  bigint generated always as identity primary key,
    nombre              varchar(120) not null,
    departamento_id     bigint       not null,
    -- Direccion de la PC destino dentro de Tailscale (host para el RDP).
    direccion_tailscale varchar(255) not null,
    -- Contrasena de la cuenta dedicada, cifrada (AES-GCM). Nula hasta que el
    -- admin la captura (caso A5). La clave de cifrado vive fuera de la base.
    credencial_cifrada  bytea,
    -- Hash del token por PC que usa el agente; nulo hasta que el agente se da de alta.
    token_agente_hash   varchar(255),
    ultimo_latido       timestamptz,
    activa              boolean      not null default true,
    constraint uq_computadora_nombre unique (nombre),
    constraint uq_computadora_direccion_tailscale unique (direccion_tailscale),
    constraint uq_computadora_token_agente_hash unique (token_agente_hash),
    constraint fk_computadora_departamento foreign key (departamento_id) references departamento (id)
);

create index ix_computadora_departamento on computadora (departamento_id);
