package com.br4y4nbv.inventario_api.repository;

import com.br4y4nbv.inventario_api.model.Movimiento;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MovimientoRepository extends JpaRepository<Movimiento, Long> {
    // Trae producto y usuario en la misma consulta: con open-in-view desactivado,
    // acceder a una relación LAZY fuera de la transacción lanzaría un error.
    @EntityGraph(attributePaths = {"producto", "usuario"})
    Page<Movimiento> findByProductoId(Long productoId, Pageable pageable);
}