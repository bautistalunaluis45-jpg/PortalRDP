namespace PortalRdpAgente;

/// <summary>Ventana simple para pegar la dirección del portal y el token de esta PC.</summary>
internal sealed class ConfigForm : Form
{
    private readonly TextBox _txtUrl = new() { Width = 360 };
    private readonly TextBox _txtToken = new() { Width = 360 };
    private readonly Configuracion _cfg;

    public ConfigForm(Configuracion cfg)
    {
        _cfg = cfg;
        Text = "Configurar agente — Portal RDP";
        FormBorderStyle = FormBorderStyle.FixedDialog;
        StartPosition = FormStartPosition.CenterScreen;
        MaximizeBox = false;
        MinimizeBox = false;
        ClientSize = new Size(420, 210);

        _txtUrl.Text = cfg.PortalUrl;
        _txtToken.Text = cfg.Token;

        var lblUrl = new Label { Text = "Dirección del portal:", Left = 20, Top = 20, AutoSize = true };
        _txtUrl.Left = 20; _txtUrl.Top = 42;
        var ejemplo = new Label
        {
            Text = "Ej.: https://serverhcpi.tailb40f15.ts.net",
            Left = 20, Top = 68, AutoSize = true, ForeColor = SystemColors.GrayText
        };

        var lblToken = new Label { Text = "Token del agente (de la pantalla de la PC en el portal):", Left = 20, Top = 100, AutoSize = true };
        _txtToken.Left = 20; _txtToken.Top = 122;

        var btnGuardar = new Button { Text = "Guardar", Left = 240, Top = 165, Width = 80, DialogResult = DialogResult.OK };
        var btnCancelar = new Button { Text = "Cancelar", Left = 330, Top = 165, Width = 80, DialogResult = DialogResult.Cancel };

        btnGuardar.Click += (_, _) =>
        {
            var url = _txtUrl.Text.Trim();
            var token = _txtToken.Text.Trim();
            if (string.IsNullOrWhiteSpace(url) || string.IsNullOrWhiteSpace(token))
            {
                MessageBox.Show(this, "Completa la dirección del portal y el token.", "Faltan datos",
                    MessageBoxButtons.OK, MessageBoxIcon.Warning);
                DialogResult = DialogResult.None;
                return;
            }
            _cfg.PortalUrl = url;
            _cfg.Token = token;
            _cfg.Guardar();
        };

        Controls.AddRange(new Control[] { lblUrl, _txtUrl, ejemplo, lblToken, _txtToken, btnGuardar, btnCancelar });
        AcceptButton = btnGuardar;
        CancelButton = btnCancelar;
    }
}
