package com.br4y4nbv.inventario_api.service;

import com.br4y4nbv.inventario_api.dto.MovimientoFiltro;
import com.br4y4nbv.inventario_api.dto.MovimientoRequest;
import com.br4y4nbv.inventario_api.dto.MovimientoResponse;
import com.br4y4nbv.inventario_api.exception.ConflictoException;
import com.br4y4nbv.inventario_api.exception.PeticionInvalidaException;
import com.br4y4nbv.inventario_api.exception.RecursoNoEncontradoException;
import com.br4y4nbv.inventario_api.model.Movimiento;
import com.br4y4nbv.inventario_api.model.Producto;
import com.br4y4nbv.inventario_api.model.TipoMovimiento;
import com.br4y4nbv.inventario_api.model.Usuario;
import com.br4y4nbv.inventario_api.repository.MovimientoRepository;
import com.br4y4nbv.inventario_api.repository.ProductoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MovimientoServiceTest {

    @Mock ProductoRepository productoRepository;
    @Mock MovimientoRepository movimientoRepository;
    @Mock UsuarioActual usuarioActual;
    @InjectMocks MovimientoService servicio;

    private Producto producto(int stock, boolean activo) {
        return Producto.builder().id(1L).sku("TEC-001").nombre("Teclado")
                .precio(new BigDecimal("1000.00")).stock(stock).stockMinimo(5).activo(activo).build();
    }

    // Simula el guardado exitoso: devuelve el mismo movimiento que recibe
    private void prepararGuardado() {
        when(usuarioActual.obtener()).thenReturn(Usuario.builder().id(7L).nombre("Ana").build());
        when(movimientoRepository.save(any(Movimiento.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void entradaSumaAlStockYRegistraElMovimiento() {
        Producto p = producto(10, true);
        when(productoRepository.findById(1L)).thenReturn(Optional.of(p));
        prepararGuardado();

        MovimientoResponse r = servicio.registrar(1L,
                new MovimientoRequest(TipoMovimiento.ENTRADA, 5, "compra"));

        assertThat(p.getStock()).isEqualTo(15);
        assertThat(r.stockResultante()).isEqualTo(15);
        assertThat(r.registradoPor()).isEqualTo("Ana");
    }

    @Test
    void salidaPuedeDejarElStockEnCero() {
        Producto p = producto(4, true);
        when(productoRepository.findById(1L)).thenReturn(Optional.of(p));
        prepararGuardado();

        servicio.registrar(1L, new MovimientoRequest(TipoMovimiento.SALIDA, 4, null));

        assertThat(p.getStock()).isZero();
    }

    @Test
    void salidaMayorAlStockSeRechazaYNoGuardaNada() {
        Producto p = producto(3, true);
        when(productoRepository.findById(1L)).thenReturn(Optional.of(p));

        assertThatThrownBy(() -> servicio.registrar(1L,
                new MovimientoRequest(TipoMovimiento.SALIDA, 5, null)))
                .isInstanceOf(ConflictoException.class)
                .hasMessageContaining("Stock insuficiente");

        assertThat(p.getStock()).isEqualTo(3);
        verifyNoInteractions(movimientoRepository, usuarioActual);
    }

    @Test
    void ajusteNegativoRestaDelStock() {
        Producto p = producto(10, true);
        when(productoRepository.findById(1L)).thenReturn(Optional.of(p));
        prepararGuardado();

        servicio.registrar(1L, new MovimientoRequest(TipoMovimiento.AJUSTE, -3, "merma"));

        assertThat(p.getStock()).isEqualTo(7);
    }

    @Test
    void unaEntradaQueDesbordaElStockSeRechaza() {
        Producto p = producto(Integer.MAX_VALUE, true);
        when(productoRepository.findById(1L)).thenReturn(Optional.of(p));

        assertThatThrownBy(() -> servicio.registrar(1L,
                new MovimientoRequest(TipoMovimiento.ENTRADA, 1, null)))
                .isInstanceOf(ConflictoException.class);

        assertThat(p.getStock()).isEqualTo(Integer.MAX_VALUE);
    }

    @Test
    void productoInactivoNoAdmiteMovimientos() {
        when(productoRepository.findById(1L)).thenReturn(Optional.of(producto(10, false)));

        assertThatThrownBy(() -> servicio.registrar(1L,
                new MovimientoRequest(TipoMovimiento.ENTRADA, 1, null)))
                .isInstanceOf(ConflictoException.class);
    }

    @Test
    void productoInexistenteLanzaNoEncontrado() {
        when(productoRepository.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.registrar(9L,
                new MovimientoRequest(TipoMovimiento.ENTRADA, 1, null)))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void elMotivoSeGuardaSinEspaciosSobrantes() {
        when(productoRepository.findById(1L)).thenReturn(Optional.of(producto(10, true)));
        prepararGuardado();

        servicio.registrar(1L, new MovimientoRequest(TipoMovimiento.ENTRADA, 1, "  compra  "));

        ArgumentCaptor<Movimiento> captor = ArgumentCaptor.forClass(Movimiento.class);
        verify(movimientoRepository).save(captor.capture());
        assertThat(captor.getValue().getMotivo()).isEqualTo("compra");
    }

    @Test
    void historialDeProductoInexistenteLanzaNoEncontrado() {
        when(productoRepository.existsById(9L)).thenReturn(false);

        assertThatThrownBy(() -> servicio.historial(9L,
                new MovimientoFiltro(null, null, null), PageRequest.of(0, 10)))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void historialConRangoDeFechasInvertidoSeRechaza() {
        when(productoRepository.existsById(1L)).thenReturn(true);

        assertThatThrownBy(() -> servicio.historial(1L,
                new MovimientoFiltro(null, LocalDate.of(2026, 12, 31), LocalDate.of(2026, 1, 1)),
                PageRequest.of(0, 10)))
                .isInstanceOf(PeticionInvalidaException.class);
    }
}