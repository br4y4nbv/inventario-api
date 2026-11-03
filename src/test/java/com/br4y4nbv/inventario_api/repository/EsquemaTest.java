package com.br4y4nbv.inventario_api.repository;

import com.br4y4nbv.inventario_api.TestcontainersConfiguration;
import com.br4y4nbv.inventario_api.model.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// Levanta el contexto completo contra una PostgreSQL en contenedor.
// Si las migraciones no coinciden con las entidades, el contexto no arranca y el test falla.
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional   // cada test se revierte al terminar: no deja datos para el siguiente
class EsquemaTest {

    @Autowired ProductoRepository productoRepository;
    @Autowired UsuarioRepository usuarioRepository;
    @Autowired MovimientoRepository movimientoRepository;

    private Producto nuevoProducto(String sku, int stock) {
        return Producto.builder()
                .sku(sku).nombre("Teclado").precio(new BigDecimal("89900.00"))
                .stock(stock).stockMinimo(5).build();
    }

    @Test
    void guardaProductoYAsignaVersionYFecha() {
        Producto p = productoRepository.saveAndFlush(nuevoProducto("TEC-001", 10));

        assertThat(p.getId()).isNotNull();
        assertThat(p.getVersion()).isNotNull();
        assertThat(p.getCreadoEn()).isNotNull();
        assertThat(productoRepository.findBySku("TEC-001")).isPresent();
    }

    @Test
    void laBaseDeDatosRechazaStockNegativo() {
        // saveAndFlush fuerza el INSERT en ese momento, para que la restricción CHECK salte aquí
        assertThatThrownBy(() -> productoRepository.saveAndFlush(nuevoProducto("TEC-002", -1)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void laBaseDeDatosRechazaSkuDuplicado() {
        productoRepository.saveAndFlush(nuevoProducto("TEC-003", 1));

        assertThatThrownBy(() -> productoRepository.saveAndFlush(nuevoProducto("TEC-003", 2)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void guardaMovimientoConSuProductoYUsuario() {
        Usuario u = usuarioRepository.saveAndFlush(Usuario.builder()
                .nombre("Ana").email("ana@test.com").password("hash").rol(Rol.ADMIN).build());
        Producto p = productoRepository.saveAndFlush(nuevoProducto("TEC-004", 10));

        movimientoRepository.saveAndFlush(Movimiento.builder()
                .producto(p).usuario(u).tipo(TipoMovimiento.ENTRADA)
                .cantidad(5).stockResultante(15).motivo("Compra a proveedor").build());

        var pagina = movimientoRepository.findByProductoId(
                p.getId(), org.springframework.data.domain.PageRequest.of(0, 10));
        assertThat(pagina.getTotalElements()).isEqualTo(1);
        assertThat(pagina.getContent().get(0).getFecha()).isNotNull();
    }

    @Test
    void laBaseDeDatosRechazaSalidaConCantidadCero() {
        Usuario u = usuarioRepository.saveAndFlush(Usuario.builder()
                .nombre("Luis").email("luis@test.com").password("hash").rol(Rol.USER).build());
        Producto p = productoRepository.saveAndFlush(nuevoProducto("TEC-005", 10));

        assertThatThrownBy(() -> movimientoRepository.saveAndFlush(Movimiento.builder()
                .producto(p).usuario(u).tipo(TipoMovimiento.SALIDA)
                .cantidad(0).stockResultante(10).build()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}