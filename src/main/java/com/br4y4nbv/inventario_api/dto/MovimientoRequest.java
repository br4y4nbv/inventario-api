package com.br4y4nbv.inventario_api.dto;

import com.br4y4nbv.inventario_api.model.TipoMovimiento;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

public record MovimientoRequest(

        @Schema(example = "ENTRADA")
        @NotNull(message = "El tipo es obligatorio")
        TipoMovimiento tipo,

        // El tope evita desbordar el INTEGER de la base de datos con una sola petición
        @Schema(example = "20")
        @NotNull(message = "La cantidad es obligatoria")
        @Min(value = -1_000_000, message = "Cantidad fuera de rango")
        @Max(value = 1_000_000, message = "Cantidad fuera de rango")
        Integer cantidad,

        @Schema(example = "Compra a Proovedor")
        @Size(max = 255, message = "Máximo 255 caracteres")
        String motivo) {

    // Validación que cruza dos campos. Si falta alguno, ya lo reportan sus @NotNull.
    @Schema(hidden = true)
    @AssertTrue(message = "ENTRADA y SALIDA requieren una cantidad positiva; AJUSTE, una distinta de cero")
    public boolean isCantidadCoherente() {
        if (tipo == null || cantidad == null) return true;
        return tipo == TipoMovimiento.AJUSTE ? cantidad != 0 : cantidad > 0;
    }
}