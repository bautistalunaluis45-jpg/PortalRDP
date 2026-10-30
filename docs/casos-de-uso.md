# Portal RDP — casos de uso por rol

Tomados de la especificación v1 y ajustados a las decisiones posteriores (28 oct 2026). «Propuesta» = aún no aprobada. El manager hace además todo lo del empleado, en todos los departamentos. El admin también se conecta, hace shadow y usa Desconectar.

## Empleado
| Código | Caso de uso | Qué ocurre y qué regla aplica |
|---|---|---|
| E1 | Ver mis computadoras | Dos consultas: PCs de su departamento y PCs con acceso temporal vigente a su nombre. Muestra libre u ocupada y quién la usa. No ve las demás ni el idle ni desde cuándo. Cuando un acceso temporal vence, la PC sale de la lista. Propuesta: si hay una sesión desconectada, mostrar "Libre · quedó abierta la sesión de X". |
| E2 | Conectarse a una PC libre | Un clic en Conectar en cualquier PC de su lista (del departamento o por acceso temporal: no hay diferencia). El helper abre mstsc sin pedir IP ni contraseña, queda un evento de lanzamiento y la PC pasa a ocupada. Conectar solo está disponible en PCs libres y el servidor lo valida. Si pierde la conexión o se sale, la PC se libera y la sesión de Windows queda abierta. |
| E3 | Editar sus preferencias RDP | Pantalla completa o ventana, monitores, tamaño/escala, sonido, micrófono, atajos de teclado y calidad (3 niveles). Las opciones que el admin bloqueó no se editan. Aplica en el siguiente lanzamiento. |
| E4 | Instalar el helper por primera vez | Descarga el instalador desde el portal, lo instala sin permisos de administrador y acepta una vez el aviso del navegador. Sin helper, Conectar muestra las instrucciones (propuesta). |
| E5 | Conexión fallida | Si la PC está apagada o el agente no reporta, aparece como sin conexión y Conectar se deshabilita. Si falla al lanzar, ve un mensaje claro y el intento queda en auditoría. |

## Manager
| Código | Caso de uso | Qué ocurre y qué regla aplica |
|---|---|---|
| M1 | Ver todas las computadoras | Todos los departamentos, con quién usa cada PC, estado (activa, desconectada, idle), tiempo idle y desde cuándo. Filtros por departamento y estado. |
| M2 | Conectarse a una PC libre de cualquier departamento | Igual que E2, sin restricción de departamento. |
| M3 | Conectarse a una PC ocupada | Desconecta al usuario actual y entra a la misma sesión abierta, no a una nueva. Propuesta: pedir confirmación antes. Auditoría: quien conecta, usuario desconectado y PC. |
| M4 | Hacer shadow a una sesión | Botón Shadow: observa la sesión solo para ver (sin `/control`). El portal lo registra; el aviso al usuario depende de la política de Windows de esa PC. Sesiones físicas: pendiente de la prueba (decisiones.md, sección 16). |
| M5 | Detectar sesiones inactivas o desconocidas | Identifica PCs con idle prolongado o sesiones "desconocido" (sin lanzamiento por el portal) y decide si hace shadow, se conecta o avisa al admin. Umbral de idle por definir. |
| M6 | Desconectar a un usuario | Desconecta al usuario de su sesión sin cerrarla (quien entra después queda donde se quedó el último). La ejecuta el agente de la PC destino; verifica que la sesión siga coincidiendo antes de desconectar. Propuesta: pedir confirmación. Queda en auditoría. |

## Admin
| Código | Caso de uso | Qué ocurre y qué regla aplica |
|---|---|---|
| A1 | Dar de alta a un usuario | Nombre, correo, departamento y rol. Sin contraseña (el login es con Google). Queda en auditoría. |
| A2 | Dar de baja o desactivar a un usuario | Pierde el acceso al portal y no puede lanzar. Si su sesión abierta se corta o no: por definir. |
| A3 | Cambiar el departamento o el rol de un usuario | Cambia de inmediato qué PCs ve y qué puede hacer. |
| A4 | Gestionar departamentos | Crear, renombrar o borrar. No se puede borrar uno con PCs o usuarios asignados hasta reasignarlos. |
| A5 | Dar de alta una PC | Nombre, departamento, dirección en Tailscale y la contraseña de la cuenta dedicada (se captura una vez y nunca vuelve a mostrarse). Verifica que el agente reporte. |
| A6 | Editar, mover o dar de baja una PC | Al cambiarla de departamento, cambia de inmediato quién la ve. |
| A7 | Actualizar la contraseña de la cuenta de una PC | Solo manual: el admin registra la nueva contraseña. No hay rotación automática (decidido). Nunca se muestra. |
| A8 | Dar un acceso temporal | Usuario, PC, vencimiento y motivo. La PC aparece en la lista del usuario. |
| A9 | Revocar un acceso temporal antes de vencer | Se bloquean nuevos lanzamientos y la PC deja de verse. La sesión abierta no se corta. |
| A10 | Consultar la auditoría | Filtra por usuario, PC, tipo de evento y fechas. Responde quién usó una PC un día, quién hizo shadow, qué accesos temporales se dieron. |
| A11 | Definir las preferencias RDP bloqueadas | Elige qué opciones sensibles no puede editar nadie: portapapeles, unidades/USB, impresoras, seriales/tarjetas inteligentes. |
| A12 | Revisar el estado de los agentes | Ve qué PCs dejaron de reportar para corregirlo antes de que un empleado lo note. |
| A13 | Conectarse, hacer shadow y Desconectar | Igual que M2, M3, M4 y M6 (confirmado que se conecta y hace shadow; si también expulsa en una PC ocupada está por confirmar). |

## Automáticos del sistema
| Código | Caso de uso | Qué ocurre y qué regla aplica |
|---|---|---|
| S1 | Vencimiento de un acceso temporal | Al llegar la fecha se bloquean nuevos lanzamientos (se calcula al consultar) y se registra el evento. En la fase 2 también se limpia la regla de Tailscale. |
| S2 | Reporte de sesiones por el agente | Cada latido actualiza libre u ocupada, idle y fin de sesión, y registra el fin de cada sesión. |
| S3 | Ejecución de órdenes del agente | Fase 3: el agente consulta órdenes pendientes (solo "desconectar sesión X"), las ejecuta y reporta el resultado. |

(Se eliminó la rotación automática de contraseña, S3 de la v1, por decisión del dueño.)
