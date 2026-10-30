package com.agentesocial.portalrdp.servicio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.agentesocial.portalrdp.dominio.AtajosRdp;
import com.agentesocial.portalrdp.dominio.CalidadRdp;
import com.agentesocial.portalrdp.dominio.PreferenciasRdp;
import com.agentesocial.portalrdp.dominio.SonidoRdp;
import com.agentesocial.portalrdp.excepcion.ReglaNegocioException;
import com.agentesocial.portalrdp.repositorio.PreferenciasRdpRepository;

@ExtendWith(MockitoExtension.class)
class PreferenciasServiceTest {

    @Mock
    private PreferenciasRdpRepository repositorio;

    private PreferenciasService servicio() {
        return new PreferenciasService(repositorio);
    }

    @Test
    void obtenerDevuelveDefaultsSiNoExiste() {
        when(repositorio.findByUsuarioId(1L)).thenReturn(Optional.empty());
        PreferenciasVista v = servicio().obtener(1L);
        assertThat(v.ajusteAutomatico()).isTrue();
        assertThat(v.escala()).isEqualTo(100);
        assertThat(v.calidad()).isEqualTo(CalidadRdp.EQUILIBRADA);
        assertThat(v.sonido()).isEqualTo(SonidoRdp.LOCAL);
    }

    @Test
    void guardarCreaCuandoNoExisteYPersisteValores() {
        when(repositorio.findByUsuarioId(1L)).thenReturn(Optional.empty());

        servicio().guardar(1L, false, true, false, 150, SonidoRdp.SILENCIADO, true,
                AtajosRdp.EQUIPO_REMOTO, CalidadRdp.MAXIMA);

        ArgumentCaptor<PreferenciasRdp> captor = ArgumentCaptor.forClass(PreferenciasRdp.class);
        verify(repositorio).save(captor.capture());
        PreferenciasRdp p = captor.getValue();
        assertThat(p.getUsuarioId()).isEqualTo(1L);
        assertThat(p.isPantallaCompleta()).isTrue();
        assertThat(p.getEscala()).isEqualTo(150);
        assertThat(p.getSonido()).isEqualTo(SonidoRdp.SILENCIADO);
        assertThat(p.getCalidad()).isEqualTo(CalidadRdp.MAXIMA);
    }

    @Test
    void rechazaEscalaInvalida() {
        assertThatThrownBy(() -> servicio().guardar(1L, true, false, true, 133,
                SonidoRdp.LOCAL, false, AtajosRdp.ESTA_PC, CalidadRdp.EQUILIBRADA))
                .isInstanceOf(ReglaNegocioException.class);
        verify(repositorio, never()).save(any());
    }
}
