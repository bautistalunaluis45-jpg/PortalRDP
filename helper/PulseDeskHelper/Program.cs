using System.Windows.Forms;

namespace PulseDeskHelper;

internal static class Program
{
    // Debe coincidir con portal.lanzamiento.protocolo del portal.
    private const string Esquema = "pulsedesk";

    [STAThread]
    private static void Main(string[] args)
    {
        ApplicationConfiguration.Initialize();
        var cfg = ConfiguracionHelper.Cargar();

        // Modo LANZAMIENTO: lo invocó el navegador con una URL pulsedesk://…
        if (args.Length > 0 && args[0].StartsWith(Esquema + "://", StringComparison.OrdinalIgnoreCase))
        {
            if (!AsegurarConfigurado(cfg))
            {
                return;
            }
            Lanzar(cfg, args[0]);
            return;
        }

        // Modo INSTALACIÓN: registrar el protocolo y pedir la URL del portal una vez.
        string exe = Environment.ProcessPath ?? Application.ExecutablePath;
        ConfiguracionHelper.RegistrarProtocolo(Esquema, exe);
        if (!AsegurarConfigurado(cfg))
        {
            return;
        }
        MessageBox.Show(
            "PulseDesk quedó instalado para tu usuario.\n\nPortal: " + cfg.PortalUrl +
            "\nVersión: " + ActualizadorHelper.VersionActual +
            "\n\nAhora, desde el portal, pulsa Conectar o Shadow y se abrirá la sesión.",
            "PulseDesk (helper)", MessageBoxButtons.OK, MessageBoxIcon.Information);

        // Al instalar, aprovecha para dejar la última versión lista (si hay una más nueva).
        ActualizadorHelper.ActualizarSiHayNueva(cfg.PortalUrl);
    }

    private static bool AsegurarConfigurado(ConfiguracionHelper cfg)
    {
        if (cfg.EstaConfigurado)
        {
            return true;
        }
        string? url = PedirTexto(
            "Dirección del portal (ej.: https://serverhcpi.tailb40f15.ts.net):",
            "Configurar PulseDesk");
        if (string.IsNullOrWhiteSpace(url))
        {
            return false;
        }
        cfg.PortalUrl = url.Trim();
        cfg.Guardar();
        return true;
    }

    private static void Lanzar(ConfiguracionHelper cfg, string url)
    {
        try
        {
            string token = ExtraerToken(url);
            if (string.IsNullOrWhiteSpace(token))
            {
                throw new InvalidOperationException("El enlace no trae token.");
            }
            var cliente = new PortalClienteHelper(cfg.PortalUrl);
            CanjeModelo canje = cliente.CanjearAsync(token).GetAwaiter().GetResult();
            Lanzador.Ejecutar(canje);
            // La sesión ya cerró (mstsc salió): es seguro buscar y preparar una actualización.
            ActualizadorHelper.ActualizarSiHayNueva(cfg.PortalUrl);
        }
        catch (Exception ex)
        {
            MessageBox.Show("No se pudo iniciar la sesión:\n\n" + ex.Message,
                "PulseDesk (helper)", MessageBoxButtons.OK, MessageBoxIcon.Warning);
        }
    }

    /// <summary>Saca el valor de token= de una URL pulsedesk://redimir?token=XYZ</summary>
    private static string ExtraerToken(string url)
    {
        int i = url.IndexOf("token=", StringComparison.OrdinalIgnoreCase);
        if (i < 0)
        {
            return "";
        }
        string resto = url[(i + "token=".Length)..];
        int fin = resto.IndexOfAny(new[] { '&', '#', '/' });
        return fin >= 0 ? resto[..fin] : resto;
    }

    /// <summary>InputBox mínimo en WinForms (sin dependencias extra).</summary>
    private static string? PedirTexto(string mensaje, string titulo)
    {
        using var f = new Form
        {
            Text = titulo,
            FormBorderStyle = FormBorderStyle.FixedDialog,
            StartPosition = FormStartPosition.CenterScreen,
            ClientSize = new Size(460, 140),
            MinimizeBox = false,
            MaximizeBox = false
        };
        var lbl = new Label { Text = mensaje, Left = 15, Top = 15, Width = 430, Height = 40 };
        var txt = new TextBox { Left = 15, Top = 60, Width = 430 };
        var ok = new Button { Text = "Guardar", Left = 270, Top = 95, Width = 80, DialogResult = DialogResult.OK };
        var cancel = new Button { Text = "Cancelar", Left = 360, Top = 95, Width = 80, DialogResult = DialogResult.Cancel };
        f.Controls.AddRange(new Control[] { lbl, txt, ok, cancel });
        f.AcceptButton = ok;
        f.CancelButton = cancel;
        return f.ShowDialog() == DialogResult.OK ? txt.Text : null;
    }
}
