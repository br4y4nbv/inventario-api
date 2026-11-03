package com.br4y4nbv.inventario_api.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "productos")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class Producto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String sku;

    private String nombre;

    private BigDecimal precio;

    @Builder.Default
    private Integer stock = 0;

    private Integer stockMinimo;

    @Builder.Default
    private boolean activo = true;

    // Bloqueo optimista: si dos transacciones modifican el mismo producto a la vez,
    // la segunda falla con OptimisticLockException en lugar de pisar el cambio de la primera.
    // Lo usaremos para que dos ventas simultáneas no dejen el stock mal calculado.
    @Version
    private Long version;

    private Instant creadoEn;

    @PrePersist
    void antesDeGuardar() {
        if (creadoEn == null) creadoEn = Instant.now();
        if (stockMinimo == null) stockMinimo = 0;
    }
}