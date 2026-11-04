package com.br4y4nbv.inventario_api.repository;

import com.br4y4nbv.inventario_api.model.Producto;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public final class ProductoSpecs {

    private ProductoSpecs() {}

    // Busca por texto en nombre o SKU (sin distinguir mayúsculas) y por estado.
    public static Specification<Producto> filtrar(String q, boolean activo) {
        return (root, query, cb) -> {
            List<Predicate> condiciones = new ArrayList<>();
            condiciones.add(cb.equal(root.get("activo"), activo));

            if (q != null && !q.isBlank()) {
                String patron = "%" + q.trim().toLowerCase() + "%";
                condiciones.add(cb.or(
                        cb.like(cb.lower(root.get("nombre")), patron),
                        cb.like(cb.lower(root.get("sku")), patron)));
            }
            return cb.and(condiciones.toArray(new Predicate[0]));
        };
    }
}