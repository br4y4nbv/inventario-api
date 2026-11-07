package com.br4y4nbv.inventario_api.dto;

import com.br4y4nbv.inventario_api.model.TipoMovimiento;

import java.time.LocalDate;

public record MovimientoFiltro(TipoMovimiento tipo, LocalDate desde, LocalDate hasta) {
}