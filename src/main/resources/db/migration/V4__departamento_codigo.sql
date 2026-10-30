-- Portal RDP - codigo corto y descripcion de departamento (pantalla admin A4).
-- El codigo (abreviacion) es el identificador corto del area; unico cuando esta
-- presente. Se agrega como nullable para no romper filas existentes; en la capa
-- de servicio se exige al crear/editar.

alter table departamento add column codigo varchar(30);
alter table departamento add column descripcion varchar(300);
alter table departamento add constraint uq_departamento_codigo unique (codigo);
