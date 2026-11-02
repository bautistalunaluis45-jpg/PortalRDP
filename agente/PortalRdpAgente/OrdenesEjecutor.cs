using System.Runtime.InteropServices;

namespace PortalRdpAgente;

/// <summary>
/// Ejecuta las órdenes que manda el portal (lista blanca). Hoy solo DESCONECTAR:
/// desconecta la sesión indicada SIN cerrarla (WTSDisconnectSession), equivalente a tsdiscon.
/// </summary>
internal static class OrdenesEjecutor
{
    public static ResultadoOrden Ejecutar(OrdenPortal orden)
    {
        if (!string.Equals(orden.Tipo, "DESCONECTAR", StringComparison.OrdinalIgnoreCase))
        {
            return new ResultadoOrden(false, "Tipo de orden no soportado: " + orden.Tipo);
        }
        if (!int.TryParse(orden.IdSesionObjetivo, out int sesionId))
        {
            return new ResultadoOrden(false, "Id de sesión inválido: " + orden.IdSesionObjetivo);
        }
        try
        {
            // bWait=false: no bloquea. Si la sesión ya no existe, la API falla → se reporta.
            bool ok = WTSDisconnectSession(WTS_CURRENT_SERVER_HANDLE, sesionId, false);
            if (ok)
            {
                return new ResultadoOrden(true, "Sesión " + sesionId + " desconectada.");
            }
            int err = Marshal.GetLastWin32Error();
            return new ResultadoOrden(false, "WTSDisconnectSession falló (código " + err + ").");
        }
        catch (Exception ex)
        {
            return new ResultadoOrden(false, "Excepción al desconectar: " + ex.Message);
        }
    }

    private static readonly IntPtr WTS_CURRENT_SERVER_HANDLE = IntPtr.Zero;

    [DllImport("wtsapi32.dll", SetLastError = true)]
    private static extern bool WTSDisconnectSession(IntPtr hServer, int sessionId, bool bWait);
}
