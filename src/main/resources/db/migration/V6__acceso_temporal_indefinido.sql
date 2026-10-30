-- Portal RDP - acceso temporal indefinido (A8).
-- vence_en pasa a ser opcional: nulo = no expira hasta que se revoque manualmente.
-- Vigente = revocado_en es nulo Y (vence_en es nulo O vence_en es futuro).

alter table acceso_temporal alter column vence_en drop not null;
