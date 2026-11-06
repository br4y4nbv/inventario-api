package com.br4y4nbv.inventario_api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// No hay campo "rol": quien se registra siempre es USER
public record RegistroRequest(

        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 100, message = "Máximo 100 caracteres")
        String nombre,

        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "Correo no válido")
        @Size(max = 150, message = "Máximo 150 caracteres")
        String email,

        // BCrypt solo procesa los primeros 72 bytes
        @NotBlank(message = "La contraseña es obligatoria")
        @Size(min = 8, max = 72, message = "Entre 8 y 72 caracteres")
        String password) {
}