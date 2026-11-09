package com.br4y4nbv.inventario_api.service;

import com.br4y4nbv.inventario_api.dto.MovimientoRequest;
import com.br4y4nbv.inventario_api.dto.MovimientoResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;

import java.util.concurrent.ThreadLocalRandom;

// Reintenta un movimiento cuando otra operación modificó el mismo producto a la vez.
// NO es @Transactional: cada intento abre su propia transacción en MovimientoService.
@Service
@RequiredArgsConstructor
public class MovimientoReintentos {

    public static final int MAX_INTENTOS = 3;

    private final MovimientoService movimientoService;

    public MovimientoResponse registrar(Long productoId, MovimientoRequest request) {
        for (int intento = 1; ; intento++) {
            try {
                return movimientoService.registrar(productoId, request);
            } catch (OptimisticLockingFailureException e) {
                // Solo se reintenta el choque de versiones. Los errores de negocio
                // (stock insuficiente, producto inactivo) se propagan: reintentarlos no cambiaría nada.
                if (intento >= MAX_INTENTOS) {
                    throw e;
                }
                esperar(intento);
            }
        }
    }

    // Espera aleatoria y creciente: evita que los hilos que chocaron vuelvan a chocar al mismo tiempo
    private void esperar(int intento) {
        try {
            Thread.sleep(ThreadLocalRandom.current().nextLong(10, 40) * intento);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrumpido mientras se reintentaba la operación", e);
        }
    }
}