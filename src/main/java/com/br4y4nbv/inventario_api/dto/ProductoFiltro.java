package com.br4y4nbv.inventario_api.dto;

import java.math.BigDecimal;

public record ProductoFiltro(String q, boolean activo, Boolean stockBajo,
                             BigDecimal precioMin, BigDecimal precioMax) {
}