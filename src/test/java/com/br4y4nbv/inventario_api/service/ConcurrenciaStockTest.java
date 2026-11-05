package com.br4y4nbv.inventario_api.service;

import com.br4y4nbv.inventario_api.TestcontainersConfiguration;
import com.br4y4nbv.inventario_api.dto.MovimientoRequest;
import com.br4y4nbv.inventario_api.exception.ConflictoException;
import com.br4y4nbv.inventario_api.model.Producto;
import com.br4y4nbv.inventario_api.model.Rol;
import com.br4y4nbv.inventario_api.model.TipoMovimiento;
import com.br4y4nbv.inventario_api.model.Usuario;
import com.br4y4nbv.inventario_api.repository.MovimientoRepository;
import com.br4y4nbv.inventario_api.repository.ProductoRepository;
import com.br4y4nbv.inventario_api.repository.UsuarioRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.OptimisticLockingFailureException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class ConcurrenciaStockTest {

    private static final int HILOS = 8;   // menos que las 10 conexiones del pool por defecto

    @Autowired MovimientoService movimientoService;
    @Autowired ProductoRepository productoRepository;
    @Autowired MovimientoRepository movimientoRepository;
    @Autowired UsuarioRepository usuarioRepository;

    @BeforeEach
    void preparar() {
        limpiar();
        // El usuario temporal se crea antes para que los hilos no compitan por crearlo
        usuarioRepository.save(Usuario.builder().nombre("Sistema")
                .email(UsuarioActual.EMAIL_TEMPORAL).password("x").rol(Rol.ADMIN).build());
    }

    @AfterEach
    void limpiar() {
        movimientoRepository.deleteAll();   // primero los movimientos: referencian a los otros dos
        productoRepository.deleteAll();
        usuarioRepository.deleteAll();
    }

    private Producto productoConStock(int stock) {
        return productoRepository.save(Producto.builder()
                .sku("CONC-001").nombre("Producto concurrente")
                .precio(new BigDecimal("1000.00")).stock(stock).stockMinimo(0).build());
    }

    // Lanza HILOS ventas de 1 unidad que arrancan exactamente a la vez.
    // Devuelve cuántas se confirmaron; las rechazadas (409 por stock o por concurrencia) no cuentan.
    private int venderEnParalelo(Long productoId) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(HILOS);
        CountDownLatch listos = new CountDownLatch(HILOS);
        CountDownLatch salida = new CountDownLatch(1);
        List<Future<Boolean>> resultados = new ArrayList<>();

        for (int i = 0; i < HILOS; i++) {
            resultados.add(pool.submit(() -> {
                listos.countDown();
                salida.await();          // todos esperan la señal para competir de verdad
                try {
                    movimientoService.registrar(productoId,
                            new MovimientoRequest(TipoMovimiento.SALIDA, 1, "venta"));
                    return true;
                } catch (ConflictoException | OptimisticLockingFailureException e) {
                    return false;
                }
            }));
        }

        listos.await();
        salida.countDown();

        int confirmadas = 0;
        for (Future<Boolean> r : resultados) {
            if (r.get(30, TimeUnit.SECONDS)) confirmadas++;
        }
        pool.shutdown();
        return confirmadas;
    }

    @Test
    void ochoVentasSimultaneasDelUltimoProductoSoloConfirmanUna() throws Exception {
        Producto p = productoConStock(1);

        int confirmadas = venderEnParalelo(p.getId());

        assertThat(confirmadas).isEqualTo(1);
        assertThat(productoRepository.findById(p.getId()).orElseThrow().getStock()).isZero();
        assertThat(movimientoRepository.count()).isEqualTo(1);
    }

    @Test
    void elStockFinalSiempreCuadraConLosMovimientosConfirmados() throws Exception {
        Producto p = productoConStock(5);

        int confirmadas = venderEnParalelo(p.getId());

        // Invariante: ninguna venta se pierde ni se duplica, y nunca hay stock negativo
        assertThat(confirmadas).isBetween(1, 5);
        assertThat(productoRepository.findById(p.getId()).orElseThrow().getStock())
                .isEqualTo(5 - confirmadas);
        assertThat(movimientoRepository.count()).isEqualTo(confirmadas);
    }
}