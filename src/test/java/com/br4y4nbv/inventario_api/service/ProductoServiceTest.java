package com.br4y4nbv.inventario_api.service;

import com.br4y4nbv.inventario_api.dto.ProductoActualizacion;
import com.br4y4nbv.inventario_api.dto.ProductoFiltro;
import com.br4y4nbv.inventario_api.dto.ProductoRequest;
import com.br4y4nbv.inventario_api.dto.ProductoResponse;
import com.br4y4nbv.inventario_api.exception.ConflictoException;
import com.br4y4nbv.inventario_api.exception.PeticionInvalidaException;
import com.br4y4nbv.inventario_api.exception.RecursoNoEncontradoException;
import com.br4y4nbv.inventario_api.model.Producto;
import com.br4y4nbv.inventario_api.repository.ProductoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductoServiceTest {

    @Mock ProductoRepository productoRepository;
    @InjectMocks ProductoService servicio;

    private Producto existente() {
        return Producto.builder().id(1L).sku("TEC-001").nombre("Viejo")
                .precio(new BigDecimal("1.00")).stock(2).stockMinimo(1).build();
    }

    @Test
    void crearNormalizaSkuYNombreYEmpiezaConStockCero() {
        when(productoRepository.existsBySku("TEC-001")).thenReturn(false);
        when(productoRepository.save(any(Producto.class))).thenAnswer(inv -> inv.getArgument(0));

        ProductoResponse r = servicio.crear(
                new ProductoRequest(" tec-001 ", "  Teclado  ", new BigDecimal("89900.00"), 5));

        assertThat(r.sku()).isEqualTo("TEC-001");
        assertThat(r.nombre()).isEqualTo("Teclado");
        assertThat(r.stock()).isZero();
        assertThat(r.activo()).isTrue();
    }

    @Test
    void crearConSkuDuplicadoSeRechazaSinGuardar() {
        when(productoRepository.existsBySku("TEC-001")).thenReturn(true);

        assertThatThrownBy(() -> servicio.crear(
                new ProductoRequest("tec-001", "Teclado", new BigDecimal("1.00"), 1)))
                .isInstanceOf(ConflictoException.class);
        verify(productoRepository, never()).save(any());
    }

    @Test
    void actualizarCambiaNombrePrecioYStockMinimoPeroNoElStock() {
        Producto p = existente();
        when(productoRepository.findById(1L)).thenReturn(Optional.of(p));

        ProductoResponse r = servicio.actualizar(1L,
                new ProductoActualizacion(" Nuevo ", new BigDecimal("5.00"), 3));

        assertThat(r.nombre()).isEqualTo("Nuevo");
        assertThat(r.precio()).isEqualByComparingTo("5.00");
        assertThat(r.stockMinimo()).isEqualTo(3);
        assertThat(p.getStock()).isEqualTo(2);
    }

    @Test
    void desactivarMarcaElProductoComoInactivo() {
        Producto p = existente();
        when(productoRepository.findById(1L)).thenReturn(Optional.of(p));

        servicio.desactivar(1L);

        assertThat(p.isActivo()).isFalse();
    }

    @Test
    void obtenerInexistenteLanzaNoEncontrado() {
        when(productoRepository.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.obtener(9L))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void listarConRangoDePreciosInvertidoSeRechazaAntesDeConsultar() {
        var filtro = new ProductoFiltro(null, true, null, new BigDecimal("9000"), new BigDecimal("1000"));

        assertThatThrownBy(() -> servicio.listar(filtro, PageRequest.of(0, 10)))
                .isInstanceOf(PeticionInvalidaException.class);
        verifyNoInteractions(productoRepository);
    }
}