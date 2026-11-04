package com.br4y4nbv.inventario_api.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record ProductoActualizacion(

        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 150, message = "Máximo 150 caracteres")
        String nombre,

        @NotNull(message = "El precio es obligatorio")
        @DecimalMin(value = "0.00", message = "El precio no puede ser negativo")
        @Digits(integer = 10, fraction = 2, message = "Máximo 10 enteros y 2 decimales")
        BigDecimal precio,

        @NotNull(message = "El stock mínimo es obligatorio")
        @PositiveOrZero(message = "El stock mínimo no puede ser negativo")
        Integer stockMinimo) {
}