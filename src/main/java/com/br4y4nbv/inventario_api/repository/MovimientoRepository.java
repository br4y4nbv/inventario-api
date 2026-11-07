package com.br4y4nbv.inventario_api.repository;

import com.br4y4nbv.inventario_api.model.Movimiento;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface MovimientoRepository
        extends JpaRepository<Movimiento, Long>, JpaSpecificationExecutor<Movimiento> {

    @EntityGraph(attributePaths = {"producto", "usuario"})
    Page<Movimiento> findByProductoId(Long productoId, Pageable pageable);

    // Se sobrescribe para cargar producto y usuario en la misma consulta
    @Override
    @EntityGraph(attributePaths = {"producto", "usuario"})
    Page<Movimiento> findAll(Specification<Movimiento> spec, Pageable pageable);
}