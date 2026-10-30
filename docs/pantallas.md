# Portal RDP — menús y pantallas

Principio del dueño: menús cortos; cada opción abre una sola pantalla con una sola tarea. Lo que no está en el menú no existe para ese rol. Las acciones son botones en la fila o ventanas pequeñas (modales), sin pantallas aparte. Las pantallas visuales ya están diseñadas por el dueño; este archivo solo documenta el comportamiento.

## Empleado (3 opciones)
| Menú | Qué muestra o permite | Casos |
|---|---|---|
| Mis computadoras | Sus PCs (departamento y acceso temporal) con libre u ocupada y quién la usa. Botón Conectar en cada PC libre. | E1, E2, E5 |
| Preferencias | Pantalla completa/ventana, monitores, tamaño/escala, sonido, micrófono, atajos, calidad. Las bloqueadas por el admin se ven bloqueadas. | E3 |
| Ayuda | Descarga e instrucciones del helper y qué hacer si no conecta. | E4, E5 |

## Manager (3 opciones)
| Menú | Qué muestra o permite | Casos |
|---|---|---|
| Computadoras | Todas las PCs con quién la usa, estado, idle y desde cuándo. Filtros por departamento y estado (libre, ocupada, idle, desconocida). En cada fila: Conectar, Shadow y Desconectar. Conectar en una PC ocupada pide confirmación antes de expulsar (propuesta). | M1 a M6 |
| Preferencias | Igual que el empleado. | E3 |
| Ayuda | Igual que el empleado. | E4 |

## Admin (6 opciones, más Preferencias y Ayuda porque también se conecta)
| Menú | Qué muestra o permite | Casos |
|---|---|---|
| Computadoras | Lista con departamento y si el agente reporta. Nueva, Editar, Baja. Dentro de Editar: cambiar departamento y actualizar la contraseña de la cuenta (se captura, nunca se muestra). Además Conectar, Shadow y Desconectar. | A5, A6, A7, A12, A13 |
| Usuarios | Lista con departamento, rol y estado. Nuevo, Editar (departamento y rol), Desactivar. | A1, A2, A3 |
| Departamentos | Lista con cuántas PCs y usuarios tiene cada uno (solo lectura). Crear, renombrar, borrar (solo si está vacío). | A4 |
| Accesos temporales | Los vigentes con usuario, PC y vencimiento. Dar acceso (usuario, PC, vencimiento, motivo) y Revocar en cada fila. | A8, A9 |
| Auditoría | Tabla de solo lectura con filtros por usuario, PC, tipo de evento y fechas. | A10 |
| Reglas RDP | Opciones sensibles con un interruptor para bloquearlas a todos. | A11 |

## Pantalla de creación de usuario (ventana pequeña)
- Campos: Nombre, Correo, Departamento (lista de los existentes), Rol (Empleado por defecto).
- Botones: Crear usuario y Cancelar.
- Validaciones: nombre y correo obligatorios, correo con formato válido y no repetido (se guarda en minúsculas), departamento existente.
- Sin campo de contraseña: el acceso es con Google Workspace.
- Sin decidir: si se envía correo de bienvenida.

## Edición de departamento
- Se edita solo el nombre.
- Se puede borrar solo si no tiene PCs ni usuarios.
- Los conteos de PCs y usuarios son de solo lectura.
- Propuesta: la auditoría guarda el nombre que tenía el departamento al momento del evento.
