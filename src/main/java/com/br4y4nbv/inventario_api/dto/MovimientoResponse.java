package com.br4y4nbv.inventario_api.dto;

import com.br4y4nbv.inventario_api.model.Movimiento;
import com.br4y4nbv.inventario_api.model.TipoMovimiento;

import java.time.Instant;

public record MovimientoResponse(
        Long id,
        Long productoId,
        String sku,
        TipoMovimiento tipo,
        Integer cantidad,
        Integer stockResultante,
        String motivo,
        String registradoPor,
        Instant fecha) {

    public static MovimientoResponse de(Movimiento m) {
        return new MovimientoResponse(m.getId(), m.getProducto().getId(), m.getProducto().getSku(),
                m.getTipo(), m.getCantidad(), m.getStockResultante(), m.getMotivo(),
                m.getUsuario().getNombre(), m.getFecha());
    }
}