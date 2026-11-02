using System.Text;
using System.Text.Json;

namespace PortalRdpAgente;

/// <summary>
/// Cliente del portal. Envía el latido (y recibe las órdenes pendientes) y reporta
/// el resultado de cada orden ejecutada. Siempre saliente hacia el portal.
/// </summary>
internal sealed class PortalCliente
{
    private static readonly HttpClient Http = new() { Timeout = TimeSpan.FromSeconds(15) };
    private static readonly JsonSerializerOptions JsonOpts = new() { PropertyNameCaseInsensitive = true };

    private readonly Configuracion _cfg;

    public PortalCliente(Configuracion cfg) => _cfg = cfg;

    /// <summary>
    /// Envía el latido y devuelve la respuesta del portal (con las órdenes pendientes),
    /// o null si el portal no aceptó el latido.
    /// </summary>
    public async Task<LatidoRespuesta?> EnviarLatidoAsync(IReadOnlyList<SesionReporte> sesiones, CancellationToken ct)
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
        if (!resp.IsSuccessStatusCode)
        {
            return null;
        }
        var json = await resp.Content.ReadAsStringAsync(ct);
        return JsonSerializer.Deserialize<LatidoRespuesta>(json, JsonOpts);
    }

    /// <summary>Reporta al portal el resultado de una orden ejecutada.</summary>
    public async Task EnviarResultadoOrdenAsync(long ordenId, bool ok, string detalle, CancellationToken ct)
    {
        var url = _cfg.PortalUrl.TrimEnd('/') + "/api/agente/ordenes/" + ordenId + "/resultado";
        using var req = new HttpRequestMessage(HttpMethod.Post, url);
        req.Headers.Add("X-Agent-Token", _cfg.Token);
        var cuerpo = new { ok, detalle };
        req.Content = new StringContent(JsonSerializer.Serialize(cuerpo), Encoding.UTF8, "application/json");
        using var resp = await Http.SendAsync(req, ct);
        _ = resp; // mejor esfuerzo: si el acuse falla, se reintenta en el próximo ciclo
    }
}
