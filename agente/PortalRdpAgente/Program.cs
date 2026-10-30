namespace PortalRdpAgente;

internal static class Program
{
    [STAThread]
    private static void Main()
    {
        // Una sola instancia por usuario.
        using var mutex = new Mutex(true, "PortalRdpAgente_SingleInstance", out bool nuevo);
        if (!nuevo)
        {
            return;
        }

        ApplicationConfiguration.Initialize();

        var cfg = Configuracion.Cargar();
        if (!cfg.EstaConfigurado)
        {
            // Primer arranque: pedir portal + token.
            using var f = new ConfigForm(cfg);
            if (f.ShowDialog() != DialogResult.OK || !cfg.EstaConfigurado)
            {
                return;
            }
        }

        Application.Run(new TrayAppContext(cfg));
    }
}
