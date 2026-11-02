# PulseDesk — enlaces importantes

Todo el portal corre detrás de Tailscale: para abrir cualquiera de estos enlaces
el equipo debe estar conectado a la red privada de la empresa.

## Enlace principal

- **Portal:** <https://serverhcpi.tailb40f15.ts.net>

  Es la dirección que se pega en el helper al instalarlo y la puerta de entrada a
  todo lo demás (requiere iniciar sesión con Google, salvo las descargas públicas).

## Descargar el agente (sin iniciar sesión)

Para instalar en las PCs destino. **No requiere login**, pensado para equipos que
todavía no tienen sesión en el portal:

- **Agente para Windows:** <https://serverhcpi.tailb40f15.ts.net/descargas/agente>

> Si responde 404, el ejecutable aún no está publicado en el servidor: hay que
> fijar la ruta del `.exe` en la variable `PORTAL_AGENTE_ARCHIVO` (propiedad
> `portal.agente.archivo`).

## Otros enlaces útiles (requieren login)

- **Descargar el helper (PulseDesk):** <https://serverhcpi.tailb40f15.ts.net/web/helper/descargar>
  — se instala una vez por usuario en el equipo del operador.
- **Guía de instalación del helper:** <https://serverhcpi.tailb40f15.ts.net/web/ayuda>
- **Iniciar sesión:** <https://serverhcpi.tailb40f15.ts.net/login>
- **Descargar el agente (vía panel admin):** <https://serverhcpi.tailb40f15.ts.net/web/admin/computadoras/agente/descargar>
  — misma descarga que la pública de arriba, pero accesible solo para admins desde el portal.
