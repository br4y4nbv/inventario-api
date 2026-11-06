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

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UsuarioActual usuarioActual;

    @PostMapping("/registro")
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioResponse registro(@Valid @RequestBody RegistroRequest request) {
        return authService.registrar(request);
    }

    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    // Requiere token: sirve para comprobar quién es el usuario y qué rol tiene
    @GetMapping("/yo")
    public UsuarioResponse yo() {
        return UsuarioResponse.de(usuarioActual.obtener());
    }
}