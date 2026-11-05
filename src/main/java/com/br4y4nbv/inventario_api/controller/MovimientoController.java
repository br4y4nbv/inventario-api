package com.br4y4nbv.inventario_api.controller;

import com.br4y4nbv.inventario_api.dto.MovimientoRequest;
import com.br4y4nbv.inventario_api.dto.MovimientoResponse;
import com.br4y4nbv.inventario_api.service.MovimientoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/productos/{productoId}/movimientos")
@RequiredArgsConstructor
public class MovimientoController {

    private final MovimientoService movimientoService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MovimientoResponse registrar(@PathVariable Long productoId,
                                        @Valid @RequestBody MovimientoRequest request) {
        return movimientoService.registrar(productoId, request);
    }

    // Más recientes primero; el id desempata movimientos con la misma fecha
    @GetMapping
    public PagedModel<MovimientoResponse> historial(@PathVariable Long productoId,
                                                    @RequestParam(defaultValue = "0") int page,
                                                    @RequestParam(defaultValue = "20") int size) {
        var pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100),
                Sort.by(Sort.Direction.DESC, "fecha").and(Sort.by(Sort.Direction.DESC, "id")));
        return new PagedModel<>(movimientoService.historial(productoId, pageable));
    }
}