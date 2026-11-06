package com.br4y4nbv.inventario_api.dto;

import com.br4y4nbv.inventario_api.model.Rol;
import com.br4y4nbv.inventario_api.model.Usuario;

public record UsuarioResponse(Long id, String nombre, String email, Rol rol) {

    public static UsuarioResponse de(Usuario u) {
        return new UsuarioResponse(u.getId(), u.getNombre(), u.getEmail(), u.getRol());
    }
}