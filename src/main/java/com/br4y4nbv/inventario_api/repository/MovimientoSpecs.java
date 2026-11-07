package com.br4y4nbv.inventario_api.repository;

import com.br4y4nbv.inventario_api.dto.MovimientoFiltro;
import com.br4y4nbv.inventario_api.model.Movimiento;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

public final class MovimientoSpecs {

    private MovimientoSpecs() {}

    // Las fechas se interpretan en UTC. "hasta" es inclusivo: llega hasta el final de ese día.
    public static Specification<Movimiento> filtrar(Long productoId, MovimientoFiltro f) {
        return (root, query, cb) -> {
            List<Predicate> condiciones = new ArrayList<>();
            condiciones.add(cb.equal(root.get("producto").get("id"), productoId));

            if (f.tipo() != null) {
                condiciones.add(cb.equal(root.get("tipo"), f.tipo()));
            }
            if (f.desde() != null) {
                condiciones.add(cb.greaterThanOrEqualTo(root.<Instant>get("fecha"),
                        f.desde().atStartOfDay(ZoneOffset.UTC).toInstant()));
            }
            if (f.hasta() != null) {
                condiciones.add(cb.lessThan(root.<Instant>get("fecha"),
                        f.hasta().plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant()));
            }
            return cb.and(condiciones.toArray(new Predicate[0]));
        };
    }
}