namespace PulseDeskHelper;

/// <summary>Lo que devuelve el portal en /api/helper/canjear (camelCase). Debe calzar con CanjeResponse del portal.</summary>
internal sealed record CanjeModelo(
    string Tipo,
    string Host,
    string? Cuenta,
    string? Password,
    string? IdSesionObjetivo,
    string? Computadora,
    bool AjusteAutomatico,
    bool PantallaCompleta,
    bool Multimonitor,
    int Escala,
    string Sonido,
    bool Microfono,
    string Atajos,
    string Calidad);
