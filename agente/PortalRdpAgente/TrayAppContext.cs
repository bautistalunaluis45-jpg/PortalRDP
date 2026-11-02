using System.Windows.Forms;

namespace PortalRdpAgente;

/// <summary>
/// Agente de bandeja: ícono junto al reloj + menú, y un bucle que cada N segundos
/// reporta las sesiones de la PC al portal. No tiene ventana principal.
/// </summary>
internal sealed class TrayAppContext : ApplicationContext
{
    private readonly Configuracion _cfg;
    private PortalCliente _cliente;
    private readonly NotifyIcon _icono;
    private readonly System.Windows.Forms.Timer _timer;
    private readonly ToolStripMenuItem _itemEstado;
    private readonly ToolStripMenuItem _itemArranque;
    private readonly CancellationTokenSource _cts = new();

    public TrayAppContext(Configuracion cfg)
    {
        _cfg = cfg;
        _cliente = new PortalCliente(cfg);

        _itemEstado = new ToolStripMenuItem("Iniciando…") { Enabled = false };
        _itemArranque = new ToolStripMenuItem("Iniciar con Windows") { CheckOnClick = true, Checked = Configuracion.ArranqueActivo() };
        _itemArranque.Click += (_, _) => Configuracion.ConfigurarArranque(_itemArranque.Checked);

        var menu = new ContextMenuStrip();
        menu.Items.Add(_itemEstado);
        menu.Items.Add(new ToolStripSeparator());
        var itemConfig = new ToolStripMenuItem("Configurar…");
        itemConfig.Click += (_, _) => Configurar();
        menu.Items.Add(itemConfig);
        menu.Items.Add(_itemArranque);
        menu.Items.Add(new ToolStripSeparator());
        var itemSalir = new ToolStripMenuItem("Salir");
        itemSalir.Click += (_, _) => Salir();
        menu.Items.Add(itemSalir);

        _icono = new NotifyIcon
        {
            Icon = SystemIcons.Application,
            Text = "Portal RDP — Agente",
            Visible = true,
            ContextMenuStrip = menu
        };

        _timer = new System.Windows.Forms.Timer { Interval = Math.Max(10, _cfg.IntervaloSegundos) * 1000 };
        _timer.Tick += async (_, _) => await LatidoAsync();
        _timer.Start();

        _ = LatidoAsync(); // primer latido inmediato
    }

    private async Task LatidoAsync()
    {
        try
        {
            var sesiones = SesionesWindows.Leer();
            var resp = await _cliente.EnviarLatidoAsync(sesiones, _cts.Token);
            string hora = DateTime.Now.ToString("HH:mm:ss");
            if (resp != null)
            {
                _itemEstado.Text = $"Conectado · {sesiones.Count} sesión(es) · {hora}";
                _icono.Text = Recortar($"Portal RDP — Sincronizado {hora}");
                await EjecutarOrdenesAsync(resp.Ordenes);
            }
            else
            {
                _itemEstado.Text = $"El portal rechazó el latido · {hora}";
                _icono.Text = Recortar("Portal RDP — Latido rechazado (revisa el token)");
            }
        }
        catch (Exception ex)
        {
            _itemEstado.Text = $"Sin conexión con el portal · {DateTime.Now:HH:mm:ss}";
            _icono.Text = Recortar("Portal RDP — Sin conexión con el portal");
            _ = ex; // detalle no se muestra para no saturar
        }
    }

    private async Task EjecutarOrdenesAsync(IReadOnlyList<OrdenPortal>? ordenes)
    {
        if (ordenes == null || ordenes.Count == 0)
        {
            return;
        }
        foreach (var orden in ordenes)
        {
            ResultadoOrden r = OrdenesEjecutor.Ejecutar(orden);
            await _cliente.EnviarResultadoOrdenAsync(orden.Id, r.Ok, r.Detalle, _cts.Token);
            _icono.ShowBalloonTip(3000, "Portal RDP",
                r.Ok ? "Orden ejecutada: " + r.Detalle : "Orden fallida: " + r.Detalle,
                r.Ok ? ToolTipIcon.Info : ToolTipIcon.Warning);
        }
    }

    private void Configurar()
    {
        using var f = new ConfigForm(_cfg);
        if (f.ShowDialog() == DialogResult.OK)
        {
            _cliente = new PortalCliente(_cfg);
            _timer.Interval = Math.Max(10, _cfg.IntervaloSegundos) * 1000;
            _ = LatidoAsync();
        }
    }

    private void Salir()
    {
        _timer.Stop();
        _cts.Cancel();
        _icono.Visible = false;
        _icono.Dispose();
        ExitThread();
    }

    // El tooltip del NotifyIcon admite máximo 63 caracteres.
    private static string Recortar(string texto) => texto.Length <= 63 ? texto : texto[..63];
}
