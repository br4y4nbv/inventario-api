package com.br4y4nbv.inventario_api.repository;

import com.br4y4nbv.inventario_api.dto.ProductoFiltro;
import com.br4y4nbv.inventario_api.model.Producto;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public final class ProductoSpecs {

    private ProductoSpecs() {}

    // Cada filtro es opcional: solo entra al WHERE si el cliente lo envió
    public static Specification<Producto> filtrar(ProductoFiltro f) {
        return (root, query, cb) -> {
            List<Predicate> condiciones = new ArrayList<>();
            condiciones.add(cb.equal(root.get("activo"), f.activo()));

            if (f.q() != null && !f.q().isBlank()) {
                String patron = "%" + f.q().trim().toLowerCase() + "%";
                condiciones.add(cb.or(
                        cb.like(cb.lower(root.get("nombre")), patron),
                        cb.like(cb.lower(root.get("sku")), patron)));
            }
            if (f.stockBajo() != null) {
                condiciones.add(f.stockBajo()
                        ? cb.lessThanOrEqualTo(root.<Integer>get("stock"), root.<Integer>get("stockMinimo"))
                        : cb.greaterThan(root.<Integer>get("stock"), root.<Integer>get("stockMinimo")));
            }
            if (f.precioMin() != null) {
                condiciones.add(cb.greaterThanOrEqualTo(root.<BigDecimal>get("precio"), f.precioMin()));
            }
            if (f.precioMax() != null) {
                condiciones.add(cb.lessThanOrEqualTo(root.<BigDecimal>get("precio"), f.precioMax()));
            }
            return cb.and(condiciones.toArray(new Predicate[0]));
        };
    }
}