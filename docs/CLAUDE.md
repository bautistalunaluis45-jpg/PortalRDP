# Portal RDP — contexto del proyecto

Portal web interno para que los empleados lancen una sesión RDP **nativa de Windows** (mstsc, no dentro del navegador) con un clic, sin conocer IPs ni credenciales. Documentos de referencia (léelos antes de proponer cambios de fondo):
- `docs/decisiones.md`: decisiones, riesgos, auditoría, fases y pendientes. Marca lo cerrado, lo propuesto y lo no verificado.
- `docs/casos-de-uso.md`: casos de uso por rol (E, M, A, S) con sus reglas.
- `docs/pantallas.md`: menús por rol y comportamiento de las pantallas.

## Componentes
- **Portal** (este repositorio): Spring Boot + PostgreSQL + Thymeleaf + HTMX. Solo accesible dentro de Tailscale (no público).
- **Agente** (.NET, servicio de Windows en cada PC destino): reporta sesiones por latido; más adelante ejecuta solo "desconectar sesión X". Conexión siempre saliente hacia el portal. Aún no existe.
- **Helper** (.NET, en la PC del empleado, instalación por usuario sin admin): protocolo URL personalizado, canjea el token, usa `cmdkey` + archivo `.rdp` + `mstsc`, borra la credencial al terminar. Aún no existe.

## Stack y versiones
- Maven (propuesta de Claude, **no confirmada por el dueño**), Java LTS vigente y Spring Boot: versiones por confirmar contra la documentación oficial al crear el proyecto. No asumir.
- Spring Security (OAuth2/OIDC con Google Workspace), Spring Data JPA, Flyway, PostgreSQL, Thymeleaf + HTMX.

## Arquitectura: monolito por capas clásicas
```
<paquete.base>
├── web/              controladores Thymeleaf/HTMX + form/
├── api/              REST para helper y agente + dto/
├── servicio/         casos de uso (@Transactional)
├── dominio/          entidades JPA y enums
├── repositorio/      Spring Data
├── seguridad/        una cadena de Spring Security por entrada
├── infraestructura/  cifrado, generador .rdp, reloj, config
└── excepcion/        excepciones de negocio + manejo global
```
Reglas:
1. Dependencias hacia abajo: `web`/`api` → `servicio` → `repositorio` → `dominio`. Un controlador nunca usa un repositorio directamente.
2. Las entidades no se exponen en vistas ni en JSON; se usan formularios y DTO.
3. Las reglas de negocio viven en los servicios. Las transacciones se abren en servicios.
4. `AuditoriaService` y `VisibilidadService` no llaman a otros servicios; los demás sí pueden llamarlos.
5. La auditoría se registra en la misma transacción que la acción.
6. Tres entradas con seguridad separada: `/web/**` (sesión Google + CSRF), `/api/helper/**` (token de lanzamiento de un solo uso), `/api/agente/**` (token por PC, guardado como hash).
7. Interfaces solo en fronteras externas (cifrado, reloj, futuro cliente de Tailscale).

## Reglas de trabajo (del dueño del proyecto)
- No mentir ni inventar: si algo no está verificado, decirlo y marcarlo. No inventar datos, rutas, versiones ni citas.
- Español latino neutro en documentación, comentarios y mensajes de la interfaz.
- **Commits y PRs: sin marca de agua ni atribución a Claude** (sin `Co-Authored-By`, sin "Generated with Claude Code", sin enlaces de sesión). Todo sale con la identidad del dueño.
- No crear documentación ni archivos que no se pidieron.
- Seguridad: nunca registrar contraseñas ni tokens en logs; la contraseña RDP solo sale del portal al canjear un token.

## Orden de construcción
1. Esqueleto, PostgreSQL (Docker Compose), Flyway `V1` con `departamento`, `usuario`, `computadora` y datos semilla.
2. Login con Google (en desarrollo, perfil con login simulado) y roles.
3. `VisibilidadService` + pantalla "Mis computadoras" (con pruebas unitarias).
4. Latido del agente (`sesion_rdp`, token por PC) probado con `curl`.
5. Lanzamiento: `lanzamiento`, `evento_auditoria`, cifrado, generador `.rdp`, canje del helper.
6. CRUD de administración (usuarios, departamentos, computadoras).
Después: agente y helper reales, preferencias RDP, accesos temporales con API de Tailscale, Desconectar.

## Pruebas
- Reglas (visibilidad, vencimientos, tokens) con reloj inyectable.
- Integración contra PostgreSQL real con las migraciones. Verificar al inicio si hay Docker disponible; si no, definir otra forma.
