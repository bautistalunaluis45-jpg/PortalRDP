-- Portal RDP - canal de órdenes hacia el agente (fase 5 / §7). Ver docs/decisiones.md 7.
-- Manager/admin encola una orden (p. ej. DESCONECTAR); el agente la recoge en su
-- latido, la ejecuta y reporta el resultado. Lista blanca de tipos.

create table orden_agente (
    id                 bigint generated always as identity primary key,
    computadora_id     bigint      not null,
    tipo               varchar(20) not null,
    id_sesion_objetivo varchar(50) not null,
    usuario_windows    varchar(200),
    estado             varchar(20) not null,
    creada_por_id      bigint      not null,
    creada_en          timestamptz not null,
    entregada_en       timestamptz,
    resuelta_en        timestamptz,
    detalle            varchar(500),
    constraint fk_orden_computadora foreign key (computadora_id) references computadora (id),
    constraint fk_orden_usuario foreign key (creada_por_id) references usuario (id),
    constraint ck_orden_tipo check (tipo in ('DESCONECTAR')),
    constraint ck_orden_estado check (estado in ('PENDIENTE', 'ENTREGADA', 'EJECUTADA', 'FALLIDA', 'EXPIRADA'))
);

create index ix_orden_pendiente on orden_agente (computadora_id) where estado = 'PENDIENTE';
