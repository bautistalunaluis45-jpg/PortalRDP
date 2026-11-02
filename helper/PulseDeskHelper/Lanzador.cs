using System.Diagnostics;
using System.Text;

namespace PulseDeskHelper;

/// <summary>
/// Abre la sesión con mstsc. Para CONECTAR: carga la credencial con cmdkey, arma un .rdp
/// con las preferencias, lanza mstsc, y al cerrarse borra la credencial y el .rdp. Para
/// SHADOW: lanza mstsc /shadow con la sesión objetivo (usa las credenciales del operador).
///
/// OJO (sin verificar en Windows): el prefijo del usuario (computadora\cuenta) y algunos
/// campos del .rdp (escala, calidad) deben confirmarse en una PC real; ver docs/decisiones.md 5, 19.
/// </summary>
internal static class Lanzador
{
    public static void Ejecutar(CanjeModelo canje)
    {
        if (string.Equals(canje.Tipo, "SHADOW", StringComparison.OrdinalIgnoreCase))
        {
            Shadow(canje);
        }
        else
        {
            Conectar(canje);
        }
    }

    private static void Conectar(CanjeModelo c)
    {
        if (string.IsNullOrWhiteSpace(c.Password))
        {
            throw new InvalidOperationException("El portal no entregó credencial para conectar.");
        }
        // Usuario para RDP: <PC>\<cuenta> (cuenta local de la PC destino). El prefijo exacto
        // puede necesitar el nombre real de Windows de la PC; se usa el nombre del portal.
        string usuario = string.IsNullOrWhiteSpace(c.Computadora) ? c.Cuenta ?? "" : $"{c.Computadora}\\{c.Cuenta}";
        string objetivo = "TERMSRV/" + c.Host;

        EjecutarProceso("cmdkey", $"/generic:{objetivo} /user:{usuario} /pass:{c.Password}");

        string rdp = Path.Combine(Path.GetTempPath(), "pulsedesk_" + Guid.NewGuid().ToString("N") + ".rdp");
        File.WriteAllText(rdp, ArmarRdp(c, usuario), Encoding.Unicode);
        try
        {
            using var mstsc = Process.Start("mstsc", $"\"{rdp}\"");
            mstsc?.WaitForExit();
        }
        finally
        {
            // Borrar la credencial al terminar (no debe quedar en la PC personal).
            EjecutarProceso("cmdkey", $"/delete:{objetivo}");
            TryBorrar(rdp);
        }
    }

    private static void Shadow(CanjeModelo c)
    {
        if (string.IsNullOrWhiteSpace(c.IdSesionObjetivo))
        {
            throw new InvalidOperationException("No se indicó la sesión a supervisar.");
        }
        // En PCs de workgroup, el shadow se autentica con la IDENTIDAD de quien lo lanza, no con
        // una contraseña de mstsc. Por eso se registra la credencial del destino con cmdkey /add
        // ANTES de lanzar (igual que un launcher probado). Sin esto da "Access is denied".
        bool conCred = !string.IsNullOrWhiteSpace(c.Password) && !string.IsNullOrWhiteSpace(c.Cuenta);
        string usuario = string.IsNullOrWhiteSpace(c.Computadora) ? (c.Cuenta ?? "") : $"{c.Computadora}\\{c.Cuenta}";
        if (conCred)
        {
            EjecutarProceso("cmdkey", $"/add:{c.Host} /user:{usuario} /pass:{c.Password}");
        }
        try
        {
            // Solo visualización (sin /control) + sin pedir consentimiento (la GPO lo permite).
            using var mstsc = Process.Start("mstsc",
                $"/v:{c.Host} /shadow:{c.IdSesionObjetivo} /noConsentPrompt");
            mstsc?.WaitForExit();
        }
        finally
        {
            if (conCred)
            {
                EjecutarProceso("cmdkey", $"/delete:{c.Host}");
            }
        }
    }

    private static string ArmarRdp(CanjeModelo c, string usuario)
    {
        var sb = new StringBuilder();
        sb.AppendLine("full address:s:" + c.Host);
        sb.AppendLine("username:s:" + usuario);
        sb.AppendLine("prompt for credentials:i:0");
        sb.AppendLine("screen mode id:i:" + (c.PantallaCompleta ? 2 : 1));
        sb.AppendLine("use multimon:i:" + (c.Multimonitor ? 1 : 0));
        sb.AppendLine("smart sizing:i:" + (c.AjusteAutomatico ? 1 : 0));
        sb.AppendLine("desktopscalefactor:i:" + (c.Escala <= 0 ? 100 : c.Escala));
        // audiomode: 0=reproducir aquí (LOCAL), 2=no reproducir (SILENCIADO)
        sb.AppendLine("audiomode:i:" + (string.Equals(c.Sonido, "SILENCIADO", StringComparison.OrdinalIgnoreCase) ? 2 : 0));
        sb.AppendLine("audiocapturemode:i:" + (c.Microfono ? 1 : 0));
        // keyboardhook: 0=esta PC, 1=equipo remoto, 2=solo pantalla completa
        sb.AppendLine("keyboardhook:i:" + c.Atajos switch
        {
            "EQUIPO_REMOTO" => 1,
            "ESTA_PC" => 0,
            _ => 2
        });
        // Perfil de calidad: cada nivel es una plantilla completa de experiencia
        // (profundidad de color, tipo de conexión, compresión, caché y efectos visuales),
        // no un solo campo. Ver docs/decisiones.md 13.
        sb.Append(BloqueCalidad(c.Calidad));
        return sb.ToString();
    }

    /// <summary>
    /// Devuelve el bloque de líneas .rdp que define la experiencia para un nivel de calidad.
    /// RAPIDA sacrifica lo visual por fluidez/ancho de banda; MAXIMA deja todo activado en LAN;
    /// EQUILIBRADA queda en medio. Valores de "connection type": 2=banda ancha lenta, 5=WAN, 6=LAN.
    /// </summary>
    private static string BloqueCalidad(string? calidad)
    {
        var sb = new StringBuilder();
        // La autodetección de red mandaría sobre el perfil fijo: la desactivamos para que
        // el nivel elegido por el usuario sea el que realmente se aplique.
        sb.AppendLine("networkautodetect:i:0");
        sb.AppendLine("bandwidthautodetect:i:0");

        switch ((calidad ?? "").ToUpperInvariant())
        {
            case "RAPIDA":
                sb.AppendLine("connection type:i:2");   // banda ancha lenta
                sb.AppendLine("session bpp:i:16");       // menos color = menos ancho de banda
                sb.AppendLine("compression:i:1");
                sb.AppendLine("bitmapcachepersistenable:i:1");
                sb.AppendLine("disable wallpaper:i:1");
                sb.AppendLine("allow font smoothing:i:0");
                sb.AppendLine("allow desktop composition:i:0");
                sb.AppendLine("disable full window drag:i:1");
                sb.AppendLine("disable menu anims:i:1");
                sb.AppendLine("disable themes:i:1");
                sb.AppendLine("disable cursor setting:i:1");
                break;

            case "MAXIMA":
                sb.AppendLine("connection type:i:6");    // LAN
                sb.AppendLine("session bpp:i:32");        // color completo
                sb.AppendLine("compression:i:1");         // sin pérdida, no afecta a la calidad
                sb.AppendLine("bitmapcachepersistenable:i:1");
                sb.AppendLine("disable wallpaper:i:0");
                sb.AppendLine("allow font smoothing:i:1");
                sb.AppendLine("allow desktop composition:i:1");
                sb.AppendLine("disable full window drag:i:0");
                sb.AppendLine("disable menu anims:i:0");
                sb.AppendLine("disable themes:i:0");
                sb.AppendLine("disable cursor setting:i:0");
                break;

            default: // EQUILIBRADA
                sb.AppendLine("connection type:i:5");    // WAN
                sb.AppendLine("session bpp:i:24");
                sb.AppendLine("compression:i:1");
                sb.AppendLine("bitmapcachepersistenable:i:1");
                sb.AppendLine("disable wallpaper:i:1");
                sb.AppendLine("allow font smoothing:i:1");
                sb.AppendLine("allow desktop composition:i:0");
                sb.AppendLine("disable full window drag:i:1");
                sb.AppendLine("disable menu anims:i:1");
                sb.AppendLine("disable themes:i:0");
                sb.AppendLine("disable cursor setting:i:0");
                break;
        }
        return sb.ToString();
    }

    private static void EjecutarProceso(string archivo, string argumentos)
    {
        var psi = new ProcessStartInfo(archivo, argumentos)
        {
            UseShellExecute = false,
            CreateNoWindow = true
        };
        using var p = Process.Start(psi);
        p?.WaitForExit();
    }

    private static void TryBorrar(string ruta)
    {
        try
        {
            if (File.Exists(ruta))
            {
                File.Delete(ruta);
            }
        }
        catch
        {
            // mejor esfuerzo
        }
    }
}
