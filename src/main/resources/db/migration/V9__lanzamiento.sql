-- Portal RDP - tokens de lanzamiento de un solo uso (fase 5). Ver docs/decisiones.md 5, 12.
-- El portal emite un token al pulsar Conectar/Shadow; el helper lo canjea UNA vez.
-- Solo se guarda el hash del token; el token en claro nunca se almacena.

create table lanzamiento (
    id                 bigint generated always as identity primary key,
    usuario_id         bigint      not null,
    computadora_id     bigint      not null,
    tipo               varchar(20) not null,
    token_hash         varchar(64) not null unique,
    estado             varchar(20) not null,
    creado_en          timestamptz not null,
    vence_en           timestamptz not null,
    canjeado_en        timestamptz,
    ip_helper          varchar(64),
    id_sesion_objetivo varchar(50),
    constraint fk_lanz_usuario foreign key (usuario_id) references usuario (id),
    constraint fk_lanz_computadora foreign key (computadora_id) references computadora (id),
    constraint ck_lanz_tipo check (tipo in ('CONECTAR', 'SHADOW')),
    constraint ck_lanz_estado check (estado in ('EMITIDO', 'CANJEADO', 'EXPIRADO', 'TERMINADO'))
);

create index ix_lanz_token on lanzamiento (token_hash);
create index ix_lanz_computadora on lanzamiento (computadora_id);
