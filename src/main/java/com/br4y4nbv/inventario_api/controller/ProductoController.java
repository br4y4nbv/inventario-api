package com.br4y4nbv.inventario_api.controller;

import com.br4y4nbv.inventario_api.dto.ProductoActualizacion;
import com.br4y4nbv.inventario_api.dto.ProductoRequest;
import com.br4y4nbv.inventario_api.dto.ProductoResponse;
import com.br4y4nbv.inventario_api.service.ProductoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/productos")
@RequiredArgsConstructor
public class ProductoController {

    private final ProductoService productoService;

    // 201 Created + cabecera Location con la URL del nuevo recurso
    @PostMapping
    public ResponseEntity<ProductoResponse> crear(@Valid @RequestBody ProductoRequest request) {
        ProductoResponse creado = productoService.crear(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(creado.id()).toUri();
        return ResponseEntity.created(location).body(creado);
    }

    @GetMapping("/{id}")
    public ProductoResponse obtener(@PathVariable Long id) {
        return productoService.obtener(id);
    }

    // GET /api/productos?q=teclado&activo=true&page=0&size=20
    @GetMapping
    public PagedModel<ProductoResponse> listar(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "true") boolean activo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        // Se limita el tamaño de página: un cliente no puede pedir 1.000.000 de filas
        var pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100),
                Sort.by("nombre"));
        // PagedModel da un JSON estable
        return new PagedModel<>(productoService.listar(q, activo, pageable));
    }

    // GET /api/productos/stock-bajo: productos que llegaron a su stock mínimo
    @GetMapping("/stock-bajo")
    public PagedModel<ProductoResponse> stockBajo(@RequestParam(defaultValue = "0") int page,
                                                @RequestParam(defaultValue = "20") int size) {
        var pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100), Sort.by("nombre"));
        return new PagedModel<>(productoService.listarStockBajo(pageable));
    }

    @PutMapping("/{id}")
    public ProductoResponse actualizar(@PathVariable Long id,
                                       @Valid @RequestBody ProductoActualizacion datos) {
        return productoService.actualizar(id, datos);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void desactivar(@PathVariable Long id) {
        productoService.desactivar(id);
    }
}