package com.br4y4nbv.inventario_api.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "usuarios")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nombre;

    private String email;

    private String password;

    // STRING guarda "ADMIN" y no un número: si cambia el orden del enum, los datos no se corrompen
    @Enumerated(EnumType.STRING)
    private Rol rol;
}