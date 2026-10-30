-- Datos semilla SOLO para desarrollo (perfil dev).
-- NO es una migracion de Flyway: lo ejecuta SeedDevRunner al arrancar en dev,
-- para no ensuciar flyway_schema_history. Es idempotente.
-- PLACEHOLDER: reemplazar por los departamentos, PCs y usuarios reales.

insert into departamento (nombre, codigo) values
    ('Soporte', 'SOP'),
    ('Ventas', 'VEN'),
    ('Administracion', 'ADM')
on conflict (nombre) do nothing;

-- Usuarios de demo (dominio del Workspace).
insert into usuario (nombre, correo, departamento_id, rol, activo)
select 'Admin Demo', 'admin@healthcarepi.com', d.id, 'ADMIN', true
from departamento d where d.nombre = 'Administracion'
on conflict (correo) do nothing;

insert into usuario (nombre, correo, departamento_id, rol, activo)
select 'Manager Demo', 'manager@healthcarepi.com', d.id, 'MANAGER', true
from departamento d where d.nombre = 'Ventas'
on conflict (correo) do nothing;

insert into usuario (nombre, correo, departamento_id, rol, activo)
select 'Empleado Demo', 'empleado@healthcarepi.com', d.id, 'EMPLEADO', true
from departamento d where d.nombre = 'Soporte'
on conflict (correo) do nothing;

-- Computadoras. ultimo_latido reciente => LIBRE; sin latido => SIN_CONEXION.
-- (Direcciones de Tailscale de ejemplo, rango CGNAT 100.64.0.0/10.)
insert into computadora (nombre, departamento_id, direccion_tailscale, cuenta_usuario, ultimo_latido, activa)
select 'PC-SOPORTE-01', d.id, '100.100.0.11', 'rdpuser', now(), true
from departamento d where d.nombre = 'Soporte'
on conflict (nombre) do nothing;

insert into computadora (nombre, departamento_id, direccion_tailscale, cuenta_usuario, ultimo_latido, activa)
select 'PC-SOPORTE-02', d.id, '100.100.0.12', 'rdpuser', null, true
from departamento d where d.nombre = 'Soporte'
on conflict (nombre) do nothing;

insert into computadora (nombre, departamento_id, direccion_tailscale, cuenta_usuario, ultimo_latido, activa)
select 'PC-VENTAS-01', d.id, '100.100.0.21', 'rdpuser', now(), true
from departamento d where d.nombre = 'Ventas'
on conflict (nombre) do nothing;

-- Acceso temporal de demo: Empleado Demo puede ver una PC de Ventas por 7 dias,
-- otorgado por Admin Demo. Idempotente (no se duplica si ya existe).
insert into acceso_temporal (usuario_id, computadora_id, vence_en, otorgado_por_id, motivo)
select emp.id, pc.id, now() + interval '7 days', adm.id, 'Soporte a Ventas (demo)'
from usuario emp, computadora pc, usuario adm
where emp.correo = 'empleado@healthcarepi.com'
  and pc.nombre = 'PC-VENTAS-01'
  and adm.correo = 'admin@healthcarepi.com'
  and not exists (
      select 1 from acceso_temporal at
      where at.usuario_id = emp.id and at.computadora_id = pc.id and at.revocado_en is null
  );
