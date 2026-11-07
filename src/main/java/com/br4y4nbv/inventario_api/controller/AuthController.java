package com.br4y4nbv.inventario_api.controller;

import com.br4y4nbv.inventario_api.dto.LoginRequest;
import com.br4y4nbv.inventario_api.dto.RegistroRequest;
import com.br4y4nbv.inventario_api.dto.TokenResponse;
import com.br4y4nbv.inventario_api.dto.UsuarioResponse;
import com.br4y4nbv.inventario_api.service.AuthService;
import com.br4y4nbv.inventario_api.service.UsuarioActual;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Autenticación")
public class AuthController {

    private final AuthService authService;
    private final UsuarioActual usuarioActual;

    @PostMapping("/registro")
    @ResponseStatus(HttpStatus.CREATED)
    @SecurityRequirements   // vacío: este endpoint es público, no pide token
    @Operation(summary = "Crear una cuenta", description = "Siempre crea un usuario con rol USER.")
    public UsuarioResponse registro(@Valid @RequestBody RegistroRequest request) {
        return authService.registrar(request);
    }

    @PostMapping("/login")
    @SecurityRequirements
    @Operation(summary = "Iniciar sesión", description = "Devuelve un token JWT con vigencia limitada.")
    public TokenResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    // Requiere token: sirve para comprobar quién es el usuario y qué rol tiene
    @GetMapping("/yo")
    @Operation(summary = "Usuario del token actual")
    public UsuarioResponse yo() {
        return UsuarioResponse.de(usuarioActual.obtener());
    }
}