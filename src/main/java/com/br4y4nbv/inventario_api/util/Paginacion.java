package com.br4y4nbv.inventario_api.util;

import com.br4y4nbv.inventario_api.exception.PeticionInvalidaException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

// Construye el Pageable a partir de parámetros del cliente, con dos protecciones:
//  - solo se puede ordenar por campos de una lista blanca (si no, ?sort=version daría un 500)
//  - el tamaño de página tiene un tope
public final class Paginacion {

    public static final int TAMANO_MAXIMO = 100;

    private Paginacion() {}

    public static Pageable de(int page, int size, String sort, String dir,
                              Set<String> permitidos, String campoPorDefecto,
                              Sort.Direction direccionPorDefecto) {

        String campo = (sort == null || sort.isBlank()) ? campoPorDefecto : sort.trim();
        if (!permitidos.contains(campo)) {
            throw new PeticionInvalidaException(
                    "Campo de orden no válido. Permitidos: " + String.join(", ", new TreeSet<>(permitidos)));
        }

        Sort.Direction direccion = direccionPorDefecto;
        if (dir != null && !dir.isBlank()) {
            direccion = switch (dir.trim().toLowerCase(Locale.ROOT)) {
                case "asc" -> Sort.Direction.ASC;
                case "desc" -> Sort.Direction.DESC;
                default -> throw new PeticionInvalidaException("Dirección de orden no válida: use asc o desc");
            };
        }

        // El id desempata para que el orden sea estable entre páginas
        Sort orden = Sort.by(direccion, campo).and(Sort.by(direccion, "id"));
        return PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), TAMANO_MAXIMO), orden);
    }
}