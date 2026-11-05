package com.br4y4nbv.inventario_api.service;

import com.br4y4nbv.inventario_api.dto.MovimientoRequest;
import com.br4y4nbv.inventario_api.dto.MovimientoResponse;
import com.br4y4nbv.inventario_api.exception.ConflictoException;
import com.br4y4nbv.inventario_api.exception.RecursoNoEncontradoException;
import com.br4y4nbv.inventario_api.model.Movimiento;
import com.br4y4nbv.inventario_api.model.Producto;
import com.br4y4nbv.inventario_api.repository.MovimientoRepository;
import com.br4y4nbv.inventario_api.repository.ProductoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MovimientoService {

    private final ProductoRepository productoRepository;
    private final MovimientoRepository movimientoRepository;
    private final UsuarioActual usuarioActual;

    // Todo ocurre en UNA transacción: o se guardan el movimiento y el nuevo stock, o ninguno.
    @Transactional
    public MovimientoResponse registrar(Long productoId, MovimientoRequest request) {
        Producto producto = productoRepository.findById(productoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Producto " + productoId + " no encontrado"));

        if (!producto.isActivo()) {
            throw new ConflictoException("El producto está inactivo y no admite movimientos");
        }

        // Variación firmada del stock
        int variacion = switch (request.tipo()) {
            case ENTRADA, AJUSTE -> request.cantidad();
            case SALIDA -> -request.cantidad();
        };

        // long evita el desbordamiento al sumar
        long nuevoStock = (long) producto.getStock() + variacion;
        if (nuevoStock < 0) {
            throw new ConflictoException("Stock insuficiente: hay " + producto.getStock()
                    + " unidades y el movimiento resta " + (-variacion));
        }
        if (nuevoStock > Integer.MAX_VALUE) {
            throw new ConflictoException("El movimiento excede el stock máximo permitido");
        }

        producto.setStock((int) nuevoStock);

        // Al cerrar la transacción, Hibernate ejecuta:
        //   UPDATE productos SET stock=?, version=version+1 WHERE id=? AND version=?
        // Si otra transacción modificó el producto entre la lectura y este momento,
        // la versión ya no coincide, el UPDATE afecta 0 filas y se lanza una
        // excepción de bloqueo optimista: la transacción se revierte completa
        // (también el movimiento) y nadie pisa el cambio del otro.
        Movimiento movimiento = movimientoRepository.save(Movimiento.builder()
                .producto(producto)
                .usuario(usuarioActual.obtener())
                .tipo(request.tipo())
                .cantidad(request.cantidad())
                .stockResultante((int) nuevoStock)
                .motivo(request.motivo() == null ? null : request.motivo().trim())
                .build());

        return MovimientoResponse.de(movimiento);
    }

    @Transactional(readOnly = true)
    public Page<MovimientoResponse> historial(Long productoId, Pageable pageable) {
        if (!productoRepository.existsById(productoId)) {
            throw new RecursoNoEncontradoException("Producto " + productoId + " no encontrado");
        }
        return movimientoRepository.findByProductoId(productoId, pageable)
                .map(MovimientoResponse::de);
    }
}