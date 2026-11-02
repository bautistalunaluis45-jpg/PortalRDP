namespace PortalRdpAgente;

/// <summary>Una sesión de Windows lista para reportar al portal.</summary>
internal sealed record SesionReporte(
    string IdSesionWindows,
    string Estado,            // "ACTIVA" | "DESCONECTADA"
    string? UsuarioWindows,
    string? IpOrigen,
    int IdleSegundos);

/// <summary>Una orden que el portal entrega en la respuesta del latido.</summary>
internal sealed record OrdenPortal(long Id, string Tipo, string? IdSesionObjetivo);

/// <summary>Respuesta del latido: incluye las órdenes pendientes para esta PC.</summary>
internal sealed record LatidoRespuesta(bool Ok, int SesionesAbiertas, IReadOnlyList<OrdenPortal>? Ordenes);

/// <summary>Resultado de ejecutar una orden localmente.</summary>
internal sealed record ResultadoOrden(bool Ok, string Detalle);
