package com.br4y4nbv.inventario_api.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "movimientos")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class Movimiento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "producto_id")
    private Producto producto;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @Enumerated(EnumType.STRING)
    private TipoMovimiento tipo;

    private Integer cantidad;

    private Integer stockResultante;

    private String motivo;

    private Instant fecha;

    @PrePersist
    void antesDeGuardar() {
        if (fecha == null) fecha = Instant.now();
    }
}