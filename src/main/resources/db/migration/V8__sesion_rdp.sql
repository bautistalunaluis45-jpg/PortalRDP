-- Portal RDP - sesiones reportadas por el agente (fase 4). Ver docs/decisiones.md 8, 12.
-- El agente de cada PC reporta por latido sus sesiones de Windows. Una sesión
-- "abierta" tiene fin_en nulo; al cerrarse (logoff) se marca fin_en.
-- usuario_atribuido_id se llenará cuando exista el lanzamiento (fase 5); por ahora
-- se muestra usuario_windows (lo que reporta el agente) o "desconocido".

create table sesion_rdp (
    id                   bigint generated always as identity primary key,
    computadora_id       bigint      not null,
    id_sesion_windows    varchar(50) not null,
    estado               varchar(20) not null,
    usuario_windows      varchar(200),
    ip_origen            varchar(64),
    desde                timestamptz not null,
    idle_segundos        int         not null default 0,
    fin_en               timestamptz,
    usuario_atribuido_id bigint,
    constraint fk_sesion_computadora foreign key (computadora_id) references computadora (id),
    constraint fk_sesion_usuario foreign key (usuario_atribuido_id) references usuario (id),
    constraint ck_sesion_estado check (estado in ('ACTIVA', 'DESCONECTADA'))
);

create index ix_sesion_computadora on sesion_rdp (computadora_id);
create index ix_sesion_abierta on sesion_rdp (computadora_id) where fin_en is null;
