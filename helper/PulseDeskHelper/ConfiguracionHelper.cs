using System.Text.Json;
using Microsoft.Win32;

namespace PulseDeskHelper;

/// <summary>
/// Configuración del helper (config.json en %APPDATA%\PulseDeskHelper) y registro del
/// protocolo URL propio (pulsedesk://) por usuario en HKCU (sin permisos de administrador).
/// El helper SOLO usa la URL del portal de su propia configuración: nunca confía en una URL
/// que venga del enlace (docs/decisiones.md 18).
/// </summary>
internal sealed class ConfiguracionHelper
{
    public string PortalUrl { get; set; } = "";

    private static string Carpeta =>
        Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData), "PulseDeskHelper");

    private static string Archivo => Path.Combine(Carpeta, "config.json");

    public static ConfiguracionHelper Cargar()
    {
        try
        {
            if (File.Exists(Archivo))
            {
                var cfg = JsonSerializer.Deserialize<ConfiguracionHelper>(File.ReadAllText(Archivo));
                if (cfg != null)
                {
                    return cfg;
                }
            }
        }
        catch
        {
            // config corrupta: se empieza de cero
        }
        return new ConfiguracionHelper();
    }

    public void Guardar()
    {
        Directory.CreateDirectory(Carpeta);
        File.WriteAllText(Archivo, JsonSerializer.Serialize(this,
            new JsonSerializerOptions { WriteIndented = true }));
    }

    public bool EstaConfigurado => !string.IsNullOrWhiteSpace(PortalUrl);

    /// <summary>Registra el esquema (p. ej. "pulsedesk") en HKCU apuntando a este ejecutable.</summary>
    public static void RegistrarProtocolo(string esquema, string rutaExe)
    {
        using var clave = Registry.CurrentUser.CreateSubKey($@"Software\Classes\{esquema}");
        clave.SetValue("", "URL:PulseDesk Protocol");
        clave.SetValue("URL Protocol", "");
        using var cmd = Registry.CurrentUser.CreateSubKey($@"Software\Classes\{esquema}\shell\open\command");
        cmd.SetValue("", $"\"{rutaExe}\" \"%1\"");
    }
}
