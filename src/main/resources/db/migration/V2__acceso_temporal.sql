-- Portal RDP - acceso temporal (fase 3, visibilidad).
-- Un acceso temporal deja ver y lanzar una PC a un usuario hasta que vence o se
-- revoca. Vigente = revocado_en es nulo y vence_en es futuro (se calcula en la
-- consulta). Ver docs/decisiones.md secciones 9 y 12.

create table acceso_temporal (
    id              bigint generated always as identity primary key,
    usuario_id      bigint      not null,
    computadora_id  bigint      not null,
    vence_en        timestamptz not null,
    -- Admin que otorgo el acceso.
    otorgado_por_id bigint      not null,
    motivo          varchar(500),
    revocado_en     timestamptz,
    constraint fk_acceso_temporal_usuario foreign key (usuario_id) references usuario (id),
    constraint fk_acceso_temporal_computadora foreign key (computadora_id) references computadora (id),
    constraint fk_acceso_temporal_otorgado_por foreign key (otorgado_por_id) references usuario (id)
);

create index ix_acceso_temporal_usuario on acceso_temporal (usuario_id);
create index ix_acceso_temporal_computadora on acceso_temporal (computadora_id);
