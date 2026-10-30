namespace PortalRdpAgente;

/// <summary>Una sesión de Windows lista para reportar al portal.</summary>
internal sealed record SesionReporte(
    string IdSesionWindows,
    string Estado,            // "ACTIVA" | "DESCONECTADA"
    string? UsuarioWindows,
    string? IpOrigen,
    int IdleSegundos);
