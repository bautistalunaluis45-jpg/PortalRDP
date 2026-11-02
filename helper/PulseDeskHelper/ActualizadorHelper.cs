using System.Diagnostics;
using System.Text.Json;

namespace PulseDeskHelper;

/// <summary>
/// Auto-actualización del helper. Consulta la versión publicada en el portal; si es
/// distinta a la de este ejecutable, baja la nueva y programa el reemplazo cuando este
/// proceso termine (un .cmd espera a que salga, hace el move y se borra). Nunca corre
/// a mitad de una conexión: se llama DESPUÉS de que mstsc cerró, o en el modo instalación.
///
/// Sin verificar en Windows (igual que el resto del helper). Ver docs/decisiones.md 19.
/// </summary>
internal static class ActualizadorHelper
{
    public const string VersionActual = "1.2.0";

    private static readonly HttpClient Http = new() { Timeout = TimeSpan.FromSeconds(30) };

    public static void ActualizarSiHayNueva(string portalUrl)
    {
        try
        {
            string exe = Environment.ProcessPath ?? "";
            if (string.IsNullOrEmpty(exe) || string.IsNullOrWhiteSpace(portalUrl))
            {
                return;
            }
            string baseUrl = portalUrl.TrimEnd('/');

            // 1) Consultar versión publicada (rápido; si falla, no pasa nada).
            string json = Http.GetStringAsync(baseUrl + "/api/helper/version").GetAwaiter().GetResult();
            string remota;
            using (var doc = JsonDocument.Parse(json))
            {
                if (!doc.RootElement.TryGetProperty("version", out var v))
                {
                    return;
                }
                remota = v.GetString() ?? "";
            }
            if (string.IsNullOrWhiteSpace(remota) || remota == VersionActual)
            {
                return; // al día (o sin versión publicada)
            }

            // 2) Bajar el nuevo ejecutable a un archivo temporal junto al actual.
            byte[] bytes = Http.GetByteArrayAsync(baseUrl + "/api/helper/descargar").GetAwaiter().GetResult();
            string nuevo = exe + ".new";
            File.WriteAllBytes(nuevo, bytes);

            // 3) Programar el reemplazo al salir este proceso.
            ProgramarReemplazo(exe, nuevo);
        }
        catch
        {
            // La auto-actualización es "mejor esfuerzo": nunca debe romper el uso normal.
        }
    }

    private static void ProgramarReemplazo(string exe, string nuevo)
    {
        int pid = Environment.ProcessId;
        string cmd = Path.Combine(Path.GetTempPath(), "pulsedesk_upd_" + pid + ".cmd");
        string script =
            "@echo off\r\n" +
            ":w\r\n" +
            "tasklist /fi \"PID eq " + pid + "\" 2>nul | find \"" + pid + "\" >nul\r\n" +
            "if not errorlevel 1 ( timeout /t 1 /nobreak >nul & goto w )\r\n" +
            "move /y \"" + nuevo + "\" \"" + exe + "\" >nul\r\n" +
            "del \"%~f0\"\r\n";
        File.WriteAllText(cmd, script);

        var psi = new ProcessStartInfo("cmd.exe", "/c \"" + cmd + "\"")
        {
            UseShellExecute = false,
            CreateNoWindow = true
        };
        Process.Start(psi);
    }
}
