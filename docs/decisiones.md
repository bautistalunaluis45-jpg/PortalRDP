# Portal RDP — decisiones de diseño

Estado al 28 de octubre de 2026. Cada punto indica si es decisión cerrada, propuesta sin confirmar, o dato no verificado.

## 1. Objetivo
Los empleados lanzan una sesión RDP normal de Windows (mstsc) desde el navegador con un clic, sin conocer IPs ni credenciales. Solo clientes Windows.

## 2. Entorno (dato del dueño)
- Las PCs de los empleados son personales.
- Las PCs destino (a las que se conectan por RDP) están en la oficina de Utah. Se asumía que se usan solo por RDP; **esto está en revisión**: el dueño quiere poder monitorear y hacer shadow también a personas sentadas frente a ellas (ver sección 16).
- No hay herramienta central de administración de PCs y no se necesita.
- Red: Tailscale. Las PCs destino tienen Tailscale instalado y ven la IP de Tailscale del cliente como origen de la conexión.

## 3. Roles (cerrado)
**Empleado**: ve solo las PCs de su departamento y las de sus accesos temporales vigentes; ve quién usa cada una; conecta a las que están libres; edita sus preferencias RDP.
**Manager**: global (todos los departamentos). Ve y conecta a todas las PCs, ve el estado de sesión (idle, desde cuándo), usa shadow, y puede conectarse a una PC ocupada: el usuario anterior es desconectado y el manager cae en la misma sesión abierta. Puede Desconectar a un usuario.
**Admin**: CRUD de usuarios, departamentos y PCs; otorga y revoca accesos temporales; consulta la auditoría; administra reglas RDP. También se conecta y hace shadow.
- El aislamiento por departamento se aplica en el servidor, no solo en la interfaz.
- Un acceso temporal que vence solo bloquea nuevos lanzamientos; no corta sesiones abiertas.
- **Pendiente de confirmar:** si el admin también puede expulsar en una PC ocupada (hoy se asume igual que el manager).

## 4. Credenciales (cerrado: "escenario B")
- Una cuenta local dedicada, sin privilegios de administrador, por cada PC destino, miembro del grupo "Usuarios de escritorio remoto" (Remote Desktop Users).
- Contraseña fija, larga y única por PC. Se guarda cifrada en el portal (AES-GCM, clave fuera de la base de datos). El admin conserva una copia en un gestor de contraseñas.
- La contraseña solo sale del portal cuando el helper canjea un token de un solo uso y corta vigencia.
- **Sin rotación de contraseñas** (decisión cerrada del dueño). El control de respaldo, si alguien extrae la contraseña, son las ACL de Tailscale.
- El firewall de Windows debe aceptar el puerto 3389 solo desde Tailscale. **Ojo con el shadow:** según una fuente de terceros (no de Microsoft), el shadow usa 445/TCP (SMB) y puertos RPC dinámicos, no el 3389. Como en el piloto funcionó, esas rutas estaban abiertas; al fijar el firewall y las reglas de Tailscale hay que permitirlas solo para manager y admin. No verificado en este entorno.
- Una sola cuenta por PC también para shadow (no hay segunda credencial).
- Estado actual (dato del dueño): las PCs destino aún no usan cuenta dedicada; hay que crearlas.

## 5. Flujo de lanzamiento
1. El empleado pulsa Conectar.
2. El portal valida rol, visibilidad y que la PC esté libre (o que sea manager/admin).
3. Registra auditoría y emite un token de un solo uso.
4. El navegador abre el protocolo URL personalizado.
5. El helper canjea el token y recibe host, cuenta, contraseña y preferencias.
6. El helper ejecuta `cmdkey /generic:TERMSRV/<host> /user:<PC>\<cuenta> /pass:<pw>`, genera el `.rdp` y lanza `mstsc`.
7. Al terminar, el helper borra la credencial (`cmdkey /delete:TERMSRV/<host>`).
- Por confirmar: vigencia del token y detalles del helper en Chrome/Edge (aviso la primera vez que se abre un protocolo externo).

## 6. Sesiones y estado
- La sesión de Windows se conserva cuando alguien se desconecta; no se cierra automáticamente. Quien entra después ve lo que dejó la persona anterior (las PCs se pasan entre personas del departamento).
- Una PC está "ocupada" solo mientras hay una sesión conectada. Si el empleado pierde la conexión o se sale, la PC aparece libre.
- Si la persona A pierde la conexión, entra B y A se reconecta sola, A saca a B. Es el comportamiento deseado; se deja la reconexión automática de mstsc por defecto.
- Propuesta sin confirmar: mostrar "Libre · quedó abierta la sesión de X" cuando hay una sesión desconectada.

## 7. Desconectar (cerrado en concepto)
- Función para manager y admin: desconecta a un usuario sin cerrar su sesión de Windows.
- La ejecuta el **agente de la PC destino** (no el helper del empleado), con `tsdiscon` o la API WTS.
- Seguridad del agente: solo conexión saliente, token por PC, lista blanca de acciones, y verifica que la sesión siga coincidiendo antes de desconectar.
- Propuesta sin confirmar: pedir confirmación antes de expulsar o desconectar.

## 8. Atribución de sesiones
- El portal conoce el lanzamiento (usuario, hora, IP de Tailscale del helper al canjear). El agente reporta las sesiones de Windows (estado, idle, desde, IP de origen del cliente).
- Se unen por IP de origen. Si no se puede atribuir, se muestra "desconocido".
- Verificado en el piloto: `netstat -n | findstr :3389` en la PC destino muestra la IP de Tailscale del cliente.
- **No verificado:** con shadow activo probablemente habrá dos conexiones al 3389.

## 9. Accesos temporales y API de Tailscale
- La visibilidad por acceso temporal se calcula en la consulta (vigente = no revocado y no vencido).
- La automatización con la API de Tailscale (editar el archivo de políticas: grupos/ACL por departamento con etiquetas) está **pendiente de probar**. Hechos verificados en la documentación oficial: el archivo de políticas se puede editar por API; el alcance necesario es `policy_file`; la sección `tests` rechaza políticas inválidas; no hay reglas con vencimiento (el portal tendría que correr una tarea de vencimiento).
- **No verificados:** límites de tasa, disponibilidad según plan, efecto de quitar una regla sobre una sesión RDP abierta, tiempo de propagación.
- Alternativa si falla: el admin edita los grupos a mano.

## 10. Login (cerrado)
- Google Workspace. Validar dominio y `email_verified` en el servidor; guardar el `sub`.
- El correo en el portal debe coincidir con la identidad de Tailscale.
- Google exige URI de redirección con HTTPS (excepción localhost), sin IP, con TLD de la lista de sufijos públicos. **No verificado:** si acepta `ts.net`.
- Opción recomendada: dominio propio (por ejemplo `portal.<dominio>`) apuntando a la IP de Tailscale del portal, con certificado de Let's Encrypt por DNS-01.
- El portal necesita reglas de Tailscale: empleados y agentes → portal:443.

## 11. Stack (cerrado)
Portal: Spring Boot, PostgreSQL, Flyway, Thymeleaf + HTMX, monolito por capas clásicas, solo interno (Tailscale). Agente y helper: .NET (elegido por Claude, a criterio del dueño). Versiones exactas por confirmar.

## 12. Modelo de dominio
Entidades: Departamento; Usuario (nombre, correo, googleSub, departamento, rol, activo); Computadora (nombre, departamento, dirección Tailscale, credencial cifrada, hash del token del agente, último latido, activa); AccesoTemporal (usuario, computadora, venceEn, otorgadoPor, motivo, revocadoEn); PreferenciasRdp (una por usuario); ReglaRdp; Lanzamiento (usuario, computadora, hash del token, creadoEn, venceEn, canjeadoEn, ipDelHelper, estado); SesionRdp (computadora, idSesiónWindows, estado, ipOrigen, desde, idle, finEn, usuarioAtribuido); EventoAuditoria (solo inserción).
Enums: Rol {EMPLEADO, MANAGER, ADMIN}; EstadoSesion {ACTIVA, DESCONECTADA}; EstadoLanzamiento {EMITIDO, CANJEADO, EXPIRADO, TERMINADO}. El estado de la computadora (LIBRE, OCUPADA, SIN_CONEXION) se calcula.
Invariantes: un usuario tiene un departamento y un rol; una PC pertenece a un departamento; el empleado solo lanza PCs libres de su lista; el token es de un solo uso y corta vigencia; la contraseña solo sale al canjear; la auditoría es inmutable.

## 13. Preferencias RDP
Editables por el usuario: pantalla completa o ventana, monitores, tamaño/escala, sonido, micrófono, atajos de teclado, calidad (3 niveles). Controladas por el admin: portapapeles, unidades/USB, impresoras, seriales/tarjetas inteligentes.

## 14. Resultados del piloto (una PC, hechos por el dueño)
1. Shadow funciona con una cuenta sin privilegios de administrador.
2. Un segundo cliente entra a la misma sesión y desconecta al primero.
3. La mecánica de lanzamiento manual (`cmdkey` + `.rdp` + `mstsc`) funciona.
4. Tailscale bloquea el 3389 entre departamentos.
5. `tsdiscon` desconecta sin cerrar la sesión y mstsc no se reconecta solo.
6. La atribución por IP funciona (`netstat`).
7. Pendiente: probar la API de Tailscale.

## 15. Valores por defecto propuestos (no confirmados)
Vigencia del token de lanzamiento: 60 s. Latido del agente: cada 30 s. "Sin conexión" tras 90 s sin latido. Auditoría sin borrado automático. Sin correo de bienvenida al crear usuario. MFA para manager/admin: decisión abierta (depende de Workspace). Todos deben ser configurables.

## 16. Sesiones físicas (idea en evaluación, NO decidida)
El dueño quiere monitorear y hacer shadow no solo a sesiones RDP sino a personas sentadas frente a la PC destino.
- Monitoreo: es muy probable que el agente pueda leer la sesión de consola (aparece como `console` en `query user`) y reportarla igual que las RDP. No verificado.
- Shadow de la sesión de consola: la documentación de `mstsc` no dice que haya restricción sobre qué sesiones se observan, pero tampoco confirma que funcione con la consola. **No verificado: es la prueba pendiente.**
- Prueba: alguien inicia sesión físicamente en una PC de prueba; `query session` da el ID de `console`; desde otro equipo, `mstsc /v:<host> /shadow:<ID> /control` (o sin `/control` para solo ver). Anotar: si conecta, si la persona recibe aviso, qué pasa al bloquear la pantalla y qué pasa con un RDP normal.
- Si funciona, habría un tercer estado de PC: "Ocupada físicamente".
- Atribución: la persona física no usa el portal; se mostraría el nombre de la cuenta de Windows con que inició sesión.
- Conflicto con el RDP (no verificado): en Windows de escritorio normalmente hay una sola sesión interactiva; un RDP podría desconectar o bloquear a quien está sentado. Propuesta sin confirmar: bloquear el lanzamiento para empleados sobre una PC ocupada físicamente y pedir confirmación a manager/admin.
- Consentimiento: por defecto Windows pide permiso a la persona observada; se puede quitar con una directiva. Observar sin que la persona lo sepa tiene implicaciones legales y laborales que dependen del lugar y de lo que haya firmado; no es una decisión técnica ni se resolvió aquí. Mínimo sensato: aviso por escrito.
- Preguntas abiertas: edición de Windows de las PCs destino (Pro o Server), quién se sienta físicamente y con qué cuenta.

## 17. Auditoría (aprobada)
Cada acción relevante deja un evento que nadie puede editar ni borrar; solo el admin lo consulta (filtros por usuario, computadora, tipo de evento y fechas). En la base de datos, rol de solo inserción.
| Evento | Qué se guarda |
|---|---|
| Entrada y salida del portal | usuario, fecha, IP de origen |
| Lanzamiento de RDP | usuario, computadora, fecha, IP de origen, resultado (incluye rechazos y fallos) |
| Fin de sesión | computadora, duración (lo reporta el agente) |
| Expulsión por conexión de un manager o admin | quien conecta, usuario desconectado, computadora |
| Desconectar | quien lo ordena, usuario desconectado, computadora |
| Shadow | quien observa, computadora, sesión observada, fecha |
| Acceso temporal creado, revocado o vencido | admin, usuario, computadora, vencimiento, motivo |
| Cambios de usuarios, computadoras y departamentos | admin, objeto, valor anterior y nuevo |
| Cambio de la contraseña de una PC | computadora y fecha; nunca la contraseña |
La retención está por definir. Propuesta: guardar el nombre del departamento tal como estaba al momento del evento.

## 18. Seguridad y riesgos
El mayor riesgo es que la contraseña de la PC destino pasa por la PC personal del empleado en cada sesión. Se contiene con la red (Tailscale y firewall), no con rotación (decidido no rotar).
| Riesgo | Mitigación |
|---|---|
| Extracción de la contraseña en una PC personal | ACL de Tailscale por departamento y accesos temporales; firewall de Windows limitado a Tailscale; contraseña larga y única por PC |
| Uso de RDP fuera del portal (no queda en la auditoría) | Misma red; las sesiones sin lanzamiento correspondiente se muestran como "desconocido" |
| Cuenta compartida: lo que se deje en la sesión lo ve la siguiente persona | Aceptado por el dueño (las sesiones se conservan a propósito); la identidad la pone el portal |
| Helper falso o enlace malicioso | Firma de código; el helper solo acepta tokens del dominio del portal; tokens de un solo uso y vida corta |
| Cuenta del portal comprometida | MFA (por decidir para manager y admin); sesiones con caducidad |
| Cuenta con privilegios de administrador | Resuelto en el piloto: el shadow funciona con una cuenta sin admin |

## 19. Helper en la PC del empleado
- Programa de Windows que se instala una vez; es el único puente entre el botón del portal y mstsc.
- Registra un protocolo URL propio (nombre por definir) por usuario, recibe el token, lo canjea, carga la credencial con `cmdkey`, lanza `mstsc` con las preferencias y borra la credencial al terminar.
- Instalación manual sin permisos de administrador (el protocolo se registra por usuario); el portal ofrece la descarga y una guía corta. No hay despliegue masivo: son PCs personales.
- Actualización: consulta al portal una versión mínima y se actualiza solo.
- Firma de código recomendada para evitar advertencias de SmartScreen.
- Navegador: la primera vez, Chrome o Edge preguntan si abrir la aplicación. Existe una política para permitirlo, pero en PCs personales probablemente no se pueda desplegar.
- No verificado: que la credencial se borre siempre si `mstsc` se cierra de forma inesperada.

## 20. Fases
1. **MVP:** roles, departamentos, CRUD de PCs, lista con estado, lanzamiento con helper y auditoría de lanzamientos.
2. **Accesos temporales y preferencias RDP,** con las reglas de Tailscale.
3. **Vista de manager, shadow y Desconectar** (canal de órdenes hacia el agente).
4. **Endurecimiento:** MFA, auto-actualización del helper y firma de código.
(La rotación de contraseña salió del plan por decisión del dueño.)

## 21. Pendientes
Decisiones:
- Confirmar o ajustar los valores del punto 15.
- Si el admin también puede expulsar en una PC ocupada.
- Aviso previo antes de que venza un acceso temporal (depende de la prueba de Tailscale).
- Qué pasa con la sesión abierta de un usuario desactivado (caso A2).
- Umbral de idle para considerar una sesión inactiva (caso M5).
- Retención de la auditoría.
- Nombre comercial, dominio del portal, nombre del protocolo del helper y **dónde se aloja el portal** (no definido; se asume dentro de Tailscale).
- Sesiones físicas (sección 16).
No verificado:
- Shadow de la sesión de consola; con shadow activo, si hay dos conexiones al 3389; puertos 445/RPC del shadow.
- Edición de Windows de las PCs destino (Pro o Server).
- API de Tailscale (agregar/quitar miembro de grupo, límites, propagación, efecto en sesión abierta); si Google acepta `ts.net`.
- Borrado de la credencial por el helper si `mstsc` se cierra mal; política de navegador en PCs personales.
Tareas de preparación:
- Proyecto de OAuth en Google Cloud; dominio y certificado del portal; certificado de firma de código.
- Cuentas dedicadas por PC con contraseñas únicas en el gestor, firewall y etiquetas/reglas de Tailscale (incluye empleados y agentes → portal:443).
- Datos semilla reales: departamentos y PCs.
- Diagrama de casos de uso: agregar Desconectar/Expulsar, Reglas RDP, Estado de agentes y Editar usuario.
