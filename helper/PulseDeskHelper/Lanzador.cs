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
        // connection type (1..7): aproxima la calidad. Por verificar el mapeo fino.
        sb.AppendLine("networkautodetect:i:0");
        sb.AppendLine("connection type:i:" + c.Calidad switch
        {
            "RAPIDA" => 2,
            "MAXIMA" => 7,
            _ => 5
        });
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
