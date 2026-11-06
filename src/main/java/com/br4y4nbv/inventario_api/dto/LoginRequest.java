package com.br4y4nbv.inventario_api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(

        @NotBlank(message = "El correo es obligatorio")
        @Size(max = 150, message = "Máximo 150 caracteres")
        String email,

        @NotBlank(message = "La contraseña es obligatoria")
        @Size(max = 72, message = "Máximo 72 caracteres")
        String password) {
}