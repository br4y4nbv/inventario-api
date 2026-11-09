package com.br4y4nbv.inventario_api.service;

import com.br4y4nbv.inventario_api.dto.MovimientoRequest;
import com.br4y4nbv.inventario_api.dto.MovimientoResponse;
import com.br4y4nbv.inventario_api.exception.ConflictoException;
import com.br4y4nbv.inventario_api.model.TipoMovimiento;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.OptimisticLockingFailureException;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MovimientoReintentosTest {

    private static final MovimientoRequest PETICION =
            new MovimientoRequest(TipoMovimiento.SALIDA, 1, "venta");
    private static final MovimientoResponse EXITO = new MovimientoResponse(
            1L, 1L, "TEC-001", TipoMovimiento.SALIDA, 1, 9, "venta", "Ana", Instant.now());

    @Mock MovimientoService movimientoService;
    @InjectMocks MovimientoReintentos reintentos;

    private static OptimisticLockingFailureException choque() {
        return new OptimisticLockingFailureException("choque de versiones");
    }

    @Test
    void reintentaHastaTenerExito() {
        when(movimientoService.registrar(1L, PETICION))
                .thenThrow(choque()).thenThrow(choque()).thenReturn(EXITO);

        assertThat(reintentos.registrar(1L, PETICION)).isSameAs(EXITO);
        verify(movimientoService, times(3)).registrar(1L, PETICION);
    }

    @Test
    void agotadosLosIntentosPropagaElConflicto() {
        when(movimientoService.registrar(1L, PETICION)).thenThrow(choque());

        assertThatThrownBy(() -> reintentos.registrar(1L, PETICION))
                .isInstanceOf(OptimisticLockingFailureException.class);
        verify(movimientoService, times(MovimientoReintentos.MAX_INTENTOS)).registrar(1L, PETICION);
    }

    @Test
    void losErroresDeNegocioNoSeReintentan() {
        when(movimientoService.registrar(1L, PETICION))
                .thenThrow(new ConflictoException("Stock insuficiente"));

        assertThatThrownBy(() -> reintentos.registrar(1L, PETICION))
                .isInstanceOf(ConflictoException.class);
        verify(movimientoService, times(1)).registrar(1L, PETICION);
    }
}