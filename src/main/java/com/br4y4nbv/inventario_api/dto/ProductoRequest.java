package com.br4y4nbv.inventario_api.dto;

import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

public record ProductoRequest(

        @Schema(example = "TEC-001")
        @NotBlank(message = "El SKU es obligatorio")
        @Size(max = 50, message = "Máximo 50 caracteres")
        @Pattern(regexp = "^[A-Za-z0-9._-]+$",
                 message = "Solo letras, números, punto, guion y guion bajo")
        String sku,

        @Schema(example = "Teclado mecánico")
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 150, message = "Máximo 150 caracteres")
        String nombre,

        @Schema(example = "89900.00")
        @NotNull(message = "El precio es obligatorio")
        @DecimalMin(value = "0.00", message = "El precio no puede ser negativo")
        @Digits(integer = 10, fraction = 2, message = "Máximo 10 enteros y 2 decimales")
        BigDecimal precio,

        @Schema(example = "5")
        @NotNull(message = "El stock mínimo es obligatorio")
        @PositiveOrZero(message = "El stock mínimo no puede ser negativo")
        Integer stockMinimo) {
}