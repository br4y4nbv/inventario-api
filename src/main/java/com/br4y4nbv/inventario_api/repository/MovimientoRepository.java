package com.br4y4nbv.inventario_api.repository;

import com.br4y4nbv.inventario_api.model.Movimiento;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MovimientoRepository extends JpaRepository<Movimiento, Long> {
    Page<Movimiento> findByProductoId(Long productoId, Pageable pageable);
}