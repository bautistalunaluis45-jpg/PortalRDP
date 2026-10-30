using System.Text;
using System.Text.Json;

namespace PortalRdpAgente;

/// <summary>Envía el latido al portal: POST {PortalUrl}/api/agente/latido con el token por PC.</summary>
internal sealed class PortalCliente
{
    private static readonly HttpClient Http = new() { Timeout = TimeSpan.FromSeconds(15) };

    private readonly Configuracion _cfg;

    public PortalCliente(Configuracion cfg) => _cfg = cfg;

    /// <summary>Devuelve true si el portal aceptó el latido (HTTP 2xx).</summary>
    public async Task<bool> EnviarLatidoAsync(IReadOnlyList<SesionReporte> sesiones, CancellationToken ct)
    {
        var cuerpo = new
        {
            sesiones = sesiones.Select(s => new
            {
                idSesionWindows = s.IdSesionWindows,
                estado = s.Estado,
                usuarioWindows = s.UsuarioWindows,
                ipOrigen = s.IpOrigen,
                idleSegundos = s.IdleSegundos
            }).ToArray()
        };

        var url = _cfg.PortalUrl.TrimEnd('/') + "/api/agente/latido";
        using var req = new HttpRequestMessage(HttpMethod.Post, url);
        req.Headers.Add("X-Agent-Token", _cfg.Token);
        req.Content = new StringContent(JsonSerializer.Serialize(cuerpo), Encoding.UTF8, "application/json");

        using var resp = await Http.SendAsync(req, ct);
        return resp.IsSuccessStatusCode;
    }
}
