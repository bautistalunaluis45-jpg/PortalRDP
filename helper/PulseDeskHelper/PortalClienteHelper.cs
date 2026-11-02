using System.Text;
using System.Text.Json;

namespace PulseDeskHelper;

/// <summary>Canjea el token de lanzamiento contra el portal configurado.</summary>
internal sealed class PortalClienteHelper
{
    private static readonly HttpClient Http = new() { Timeout = TimeSpan.FromSeconds(15) };
    private static readonly JsonSerializerOptions JsonOpts = new() { PropertyNameCaseInsensitive = true };

    private readonly string _portalUrl;

    public PortalClienteHelper(string portalUrl) => _portalUrl = portalUrl;

    /// <summary>POST {portal}/api/helper/canjear con el token. Lanza si el portal responde error.</summary>
    public async Task<CanjeModelo> CanjearAsync(string token)
    {
        var url = _portalUrl.TrimEnd('/') + "/api/helper/canjear";
        var cuerpo = JsonSerializer.Serialize(new { token });
        using var resp = await Http.PostAsync(url, new StringContent(cuerpo, Encoding.UTF8, "application/json"));
        var json = await resp.Content.ReadAsStringAsync();
        if (!resp.IsSuccessStatusCode)
        {
            string msg = json;
            try
            {
                using var doc = JsonDocument.Parse(json);
                if (doc.RootElement.TryGetProperty("error", out var e))
                {
                    msg = e.GetString() ?? json;
                }
            }
            catch
            {
                // cuerpo no-JSON: se usa tal cual
            }
            throw new InvalidOperationException(msg);
        }
        return JsonSerializer.Deserialize<CanjeModelo>(json, JsonOpts)
               ?? throw new InvalidOperationException("Respuesta de canje vacía.");
    }
}
