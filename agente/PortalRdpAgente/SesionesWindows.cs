using System.Diagnostics;
using System.Runtime.InteropServices;

namespace PortalRdpAgente;

/// <summary>
/// Lee las sesiones de Windows de esta PC vía la API WTS (como "query user") y
/// las traduce al formato que espera el portal. Solo reporta sesiones con usuario
/// en estado Activa o Desconectada. El idle se calcula para la sesión interactiva
/// de este proceso (GetLastInputInfo); para otras sesiones se reporta 0.
/// </summary>
internal static class SesionesWindows
{
    public static IReadOnlyList<SesionReporte> Leer()
    {
        var resultado = new List<SesionReporte>();
        IntPtr servidor = WTS_CURRENT_SERVER_HANDLE;
        int sesionActual = Process.GetCurrentProcess().SessionId;

        if (!WTSEnumerateSessions(servidor, 0, 1, out IntPtr ppSesiones, out int conteo))
        {
            return resultado;
        }

        try
        {
            int tam = Marshal.SizeOf<WTS_SESSION_INFO>();
            for (int i = 0; i < conteo; i++)
            {
                IntPtr dir = ppSesiones + i * tam;
                var info = Marshal.PtrToStructure<WTS_SESSION_INFO>(dir);

                string? usuario = ConsultarTexto(servidor, info.SessionId, WTS_INFO_CLASS.WTSUserName);
                if (string.IsNullOrWhiteSpace(usuario))
                {
                    continue; // sesiones de servicio/listen/console sin usuario: se ignoran
                }

                string? estado = MapearEstado((WTS_CONNECTSTATE_CLASS)info.State);
                if (estado == null)
                {
                    continue; // solo Activa/Desconectada
                }

                string? dominio = ConsultarTexto(servidor, info.SessionId, WTS_INFO_CLASS.WTSDomainName);
                string usuarioCompleto = string.IsNullOrWhiteSpace(dominio) ? usuario! : $"{dominio}\\{usuario}";
                string? ip = ConsultarIp(servidor, info.SessionId);
                int idle = info.SessionId == sesionActual ? IdleSegundosSesionActual() : 0;

                resultado.Add(new SesionReporte(
                    info.SessionId.ToString(), estado, usuarioCompleto, ip, idle));
            }
        }
        finally
        {
            WTSFreeMemory(ppSesiones);
        }

        return resultado;
    }

    private static string? MapearEstado(WTS_CONNECTSTATE_CLASS estado) => estado switch
    {
        WTS_CONNECTSTATE_CLASS.WTSActive => "ACTIVA",
        WTS_CONNECTSTATE_CLASS.WTSDisconnected => "DESCONECTADA",
        _ => null
    };

    private static string? ConsultarTexto(IntPtr servidor, int sesion, WTS_INFO_CLASS clase)
    {
        if (WTSQuerySessionInformation(servidor, sesion, clase, out IntPtr buf, out _))
        {
            try { return Marshal.PtrToStringUni(buf); }
            finally { WTSFreeMemory(buf); }
        }
        return null;
    }

    private static string? ConsultarIp(IntPtr servidor, int sesion)
    {
        if (!WTSQuerySessionInformation(servidor, sesion, WTS_INFO_CLASS.WTSClientAddress, out IntPtr buf, out _))
        {
            return null;
        }
        try
        {
            var dir = Marshal.PtrToStructure<WTS_CLIENT_ADDRESS>(buf);
            // AF_INET = 2; los 4 octetos de IPv4 están en Address[2..5].
            if (dir.AddressFamily == 2)
            {
                string ip = $"{dir.Address[2]}.{dir.Address[3]}.{dir.Address[4]}.{dir.Address[5]}";
                return ip == "0.0.0.0" ? null : ip;
            }
            return null;
        }
        finally
        {
            WTSFreeMemory(buf);
        }
    }

    private static int IdleSegundosSesionActual()
    {
        var lii = new LASTINPUTINFO { cbSize = (uint)Marshal.SizeOf<LASTINPUTINFO>() };
        if (!GetLastInputInfo(ref lii))
        {
            return 0;
        }
        long ms = (uint)Environment.TickCount - lii.dwTime;
        if (ms < 0) ms = 0;
        return (int)(ms / 1000);
    }

    // --- P/Invoke ---

    private static readonly IntPtr WTS_CURRENT_SERVER_HANDLE = IntPtr.Zero;

    private enum WTS_INFO_CLASS
    {
        WTSUserName = 5,
        WTSDomainName = 7,
        WTSConnectState = 8,
        WTSClientAddress = 14
    }

    private enum WTS_CONNECTSTATE_CLASS
    {
        WTSActive = 0,
        WTSConnected = 1,
        WTSConnectQuery = 2,
        WTSShadow = 3,
        WTSDisconnected = 4,
        WTSIdle = 5,
        WTSListen = 6,
        WTSReset = 7,
        WTSDown = 8,
        WTSInit = 9
    }

    [StructLayout(LayoutKind.Sequential)]
    private struct WTS_SESSION_INFO
    {
        public int SessionId;
        public IntPtr pWinStationName;
        public int State;
    }

    [StructLayout(LayoutKind.Sequential)]
    private struct WTS_CLIENT_ADDRESS
    {
        public int AddressFamily;
        [MarshalAs(UnmanagedType.ByValArray, SizeConst = 20)]
        public byte[] Address;
    }

    [StructLayout(LayoutKind.Sequential)]
    private struct LASTINPUTINFO
    {
        public uint cbSize;
        public uint dwTime;
    }

    [DllImport("wtsapi32.dll", SetLastError = true)]
    private static extern bool WTSEnumerateSessions(IntPtr hServer, int reserved, int version,
        out IntPtr ppSessionInfo, out int pCount);

    [DllImport("wtsapi32.dll")]
    private static extern void WTSFreeMemory(IntPtr pMemory);

    [DllImport("wtsapi32.dll", SetLastError = true, CharSet = CharSet.Unicode)]
    private static extern bool WTSQuerySessionInformation(IntPtr hServer, int sessionId,
        WTS_INFO_CLASS wtsInfoClass, out IntPtr ppBuffer, out int pBytesReturned);

    [DllImport("user32.dll")]
    private static extern bool GetLastInputInfo(ref LASTINPUTINFO plii);
}
