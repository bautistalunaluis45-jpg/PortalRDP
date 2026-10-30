-- Portal RDP - cuenta dedicada por computadora (pantalla admin A5).
-- Nombre de usuario de la cuenta local dedicada de cada PC (no es secreto; el
-- secreto es la contraseña, que va cifrada en credencial_cifrada). Se usa al
-- lanzar: cmdkey /user:<PC>\<cuenta>. Ver docs/decisiones.md secciones 4 y 5.

alter table computadora add column cuenta_usuario varchar(100);
