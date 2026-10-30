using System.Text.Json;
using Microsoft.Win32;

namespace PortalRdpAgente;

/// <summary>
/// Configuración del agente, guardada en %APPDATA%\PortalRdpAgente\config.json.
/// También gestiona el arranque con Windows (clave Run del usuario, sin admin).
/// </summary>
internal sealed class Configuracion
{
    public string PortalUrl { get; set; } = "";
    public string Token { get; set; } = "";
    public int IntervaloSegundos { get; set; } = 30;

    private const string ClaveRun = @"Software\Microsoft\Windows\CurrentVersion\Run";
    private const string NombreApp = "PortalRdpAgente";

    private static string Carpeta =>
        Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData), "PortalRdpAgente");

    private static string RutaConfig => Path.Combine(Carpeta, "config.json");

    public static Configuracion Cargar()
    {
        try
        {
            if (File.Exists(RutaConfig))
            {
                var json = File.ReadAllText(RutaConfig);
                var cfg = JsonSerializer.Deserialize<Configuracion>(json);
                if (cfg != null)
                {
                    if (cfg.IntervaloSegundos < 10) cfg.IntervaloSegundos = 30;
                    return cfg;
                }
            }
        }
        catch
        {
            // config corrupta: se usa una nueva
        }
        return new Configuracion();
    }

    public void Guardar()
    {
        Directory.CreateDirectory(Carpeta);
        var json = JsonSerializer.Serialize(this, new JsonSerializerOptions { WriteIndented = true });
        File.WriteAllText(RutaConfig, json);
    }

    public bool EstaConfigurado => !string.IsNullOrWhiteSpace(PortalUrl) && !string.IsNullOrWhiteSpace(Token);

    // --- Arranque con Windows (por usuario, sin privilegios de administrador) ---

    public static bool ArranqueActivo()
    {
        using var k = Registry.CurrentUser.OpenSubKey(ClaveRun, false);
        return k?.GetValue(NombreApp) != null;
    }

    public static void ConfigurarArranque(bool activar)
    {
        using var k = Registry.CurrentUser.OpenSubKey(ClaveRun, true)
                      ?? Registry.CurrentUser.CreateSubKey(ClaveRun);
        if (activar)
        {
            k!.SetValue(NombreApp, $"\"{Application.ExecutablePath}\"");
        }
        else
        {
            k!.DeleteValue(NombreApp, false);
        }
    }
}
