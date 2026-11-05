package com.br4y4nbv.inventario_api.repository;

import com.br4y4nbv.inventario_api.model.Producto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface ProductoRepository
        extends JpaRepository<Producto, Long>, JpaSpecificationExecutor<Producto> {

    Optional<Producto> findBySku(String sku);

    boolean existsBySku(String sku);

    // Productos activos cuyo stock llegó al mínimo o quedó por debajo
    @Query("select p from Producto p where p.activo = true and p.stock <= p.stockMinimo")
    Page<Producto> findStockBajo(Pageable pageable);
}