package com.br4y4nbv.inventario_api.service;

import com.br4y4nbv.inventario_api.model.Rol;
import com.br4y4nbv.inventario_api.model.Usuario;
import com.br4y4nbv.inventario_api.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

// TEMPORAL
// Mientras tanto, todos los movimientos se atribuyen a un usuario "sistema".
@Component
@RequiredArgsConstructor
public class UsuarioActual {

    public static final String EMAIL_TEMPORAL = "sistema@inventario.local";

    private final UsuarioRepository usuarioRepository;

    public Usuario obtener() {
        return usuarioRepository.findByEmail(EMAIL_TEMPORAL)
                .orElseGet(() -> usuarioRepository.save(Usuario.builder()
                        .nombre("Sistema")
                        .email(EMAIL_TEMPORAL)
                        .password("sin-acceso")   // no es un hash válido: este usuario no puede iniciar sesión
                        .rol(Rol.ADMIN)
                        .build()));
    }
}