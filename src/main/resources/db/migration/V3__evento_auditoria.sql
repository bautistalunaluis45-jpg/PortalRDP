-- Portal RDP - auditoria (fase admin). Ver docs/decisiones.md seccion 17.
-- Tabla de solo insercion: nadie edita ni borra. El control a nivel de base
-- (rol con solo INSERT/SELECT) queda como tarea de endurecimiento del despliegue.
-- Se guardan datos denormalizados (correo del actor, nombre del departamento) para
-- que el registro siga siendo legible aunque el usuario o el departamento cambien.

create table evento_auditoria (
    id                  bigint generated always as identity primary key,
    tipo                varchar(40)  not null,
    ocurrido_en         timestamptz  not null,
    -- Quien ejecuto la accion; nulo para eventos del sistema.
    actor_usuario_id    bigint,
    actor_correo        varchar(254),
    -- Usuario sobre el que recae la accion (alta, cambio, desconexion, etc.).
    usuario_afectado_id bigint,
    computadora_id      bigint,
    departamento_nombre varchar(120),
    ip_origen           varchar(64),
    detalle             text,
    constraint fk_evento_actor foreign key (actor_usuario_id) references usuario (id),
    constraint fk_evento_afectado foreign key (usuario_afectado_id) references usuario (id),
    constraint fk_evento_computadora foreign key (computadora_id) references computadora (id)
);

create index ix_evento_ocurrido on evento_auditoria (ocurrido_en);
create index ix_evento_tipo on evento_auditoria (tipo);
create index ix_evento_actor on evento_auditoria (actor_usuario_id);
create index ix_evento_computadora on evento_auditoria (computadora_id);
