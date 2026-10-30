-- Portal RDP - preferencias RDP por usuario (caso E3, docs/decisiones.md 13).
-- Una fila por usuario (unique). Se aplican en el siguiente lanzamiento.

create table preferencias_rdp (
    id                bigint generated always as identity primary key,
    usuario_id        bigint  not null,
    ajuste_automatico boolean not null default true,
    pantalla_completa boolean not null default false,
    multimonitor      boolean not null default true,
    escala            int     not null default 100,
    sonido            varchar(20) not null default 'LOCAL',
    microfono         boolean not null default false,
    atajos            varchar(30) not null default 'SOLO_PANTALLA_COMPLETA',
    calidad           varchar(20) not null default 'EQUILIBRADA',
    constraint uq_preferencias_usuario unique (usuario_id),
    constraint fk_preferencias_usuario foreign key (usuario_id) references usuario (id),
    constraint ck_pref_sonido check (sonido in ('LOCAL', 'SILENCIADO')),
    constraint ck_pref_atajos check (atajos in ('ESTA_PC', 'EQUIPO_REMOTO', 'SOLO_PANTALLA_COMPLETA')),
    constraint ck_pref_calidad check (calidad in ('EQUILIBRADA', 'RAPIDA', 'MAXIMA'))
);
