package com.br4y4nbv.inventario_api.dto;

import com.br4y4nbv.inventario_api.model.Producto;

import java.math.BigDecimal;

public record ProductoResponse(
        Long id,
        String sku,
        String nombre,
        BigDecimal precio,
        Integer stock,
        Integer stockMinimo,
        boolean stockBajo,
        boolean activo) {

    // La API nunca devuelve la entidad directamente: así el contrato JSON
    // no depende de cómo esté guardada en la base de datos.
    public static ProductoResponse de(Producto p) {
        return new ProductoResponse(p.getId(), p.getSku(), p.getNombre(), p.getPrecio(),
                p.getStock(), p.getStockMinimo(), p.getStock() <= p.getStockMinimo(), p.isActivo());
    }
}