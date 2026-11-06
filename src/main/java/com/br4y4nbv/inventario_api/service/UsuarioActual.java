package com.br4y4nbv.inventario_api.service;

import com.br4y4nbv.inventario_api.model.Usuario;
import com.br4y4nbv.inventario_api.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

// Devuelve el usuario de la petición actual a partir del JWT ya verificado por Spring Security.
@Component
@RequiredArgsConstructor
public class UsuarioActual {

    private final UsuarioRepository usuarioRepository;

    public Usuario obtener() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new BadCredentialsException("No hay un usuario autenticado");
        }
        // getName() es el "sub" del token: el correo
        return usuarioRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new BadCredentialsException("El usuario del token ya no existe"));
    }
}