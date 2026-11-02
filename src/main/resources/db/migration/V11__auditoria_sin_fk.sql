-- Portal RDP - desacoplar la auditoría de las tablas vivas (borrado definitivo de PC/usuario).
-- La bitácora (evento_auditoria) es de solo inserción y ya guarda datos denormalizados
-- (correo del actor, nombre del departamento) para seguir siendo legible aunque cambie el
-- origen. Para poder ELIMINAR por completo un usuario o una computadora SIN perder sus
-- registros de auditoría, quitamos las claves foráneas que ataban la bitácora a usuario/
-- computadora. Las columnas (actor_usuario_id, usuario_afectado_id, computadora_id) y sus
-- índices se conservan: siguen sirviendo para filtrar y enlazar, solo que ya no impiden el
-- borrado del origen. Ver docs/decisiones.md 17.

alter table evento_auditoria drop constraint fk_evento_actor;
alter table evento_auditoria drop constraint fk_evento_afectado;
alter table evento_auditoria drop constraint fk_evento_computadora;
