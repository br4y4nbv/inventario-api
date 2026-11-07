package com.br4y4nbv.inventario_api.controller;

import com.br4y4nbv.inventario_api.dto.ProductoActualizacion;
import com.br4y4nbv.inventario_api.dto.ProductoFiltro;
import com.br4y4nbv.inventario_api.dto.ProductoRequest;
import com.br4y4nbv.inventario_api.dto.ProductoResponse;
import com.br4y4nbv.inventario_api.service.ProductoService;
import com.br4y4nbv.inventario_api.util.Paginacion;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.math.BigDecimal;
import java.net.URI;
import java.util.Set;

@RestController
@RequestMapping("/api/productos")
@RequiredArgsConstructor
@Tag(name = "Productos",
     description = "Consultar: USER o ADMIN. Crear, editar y desactivar: solo ADMIN.")
public class ProductoController {

    // Lista blanca: son las únicas columnas por las que se permite ordenar
    private static final Set<String> CAMPOS_ORDEN = Set.of("nombre", "sku", "precio", "stock", "creadoEn");

    private final ProductoService productoService;

    @PostMapping
    @Operation(summary = "Crear un producto",
               description = "Solo ADMIN. El stock inicial es 0: solo cambia mediante movimientos.")
    @ApiResponse(responseCode = "201", description = "Producto creado (cabecera Location con su URL)")
    @ApiResponse(responseCode = "400", description = "Datos inválidos")
    @ApiResponse(responseCode = "409", description = "Ya existe un producto con ese SKU")
    public ResponseEntity<ProductoResponse> crear(@Valid @RequestBody ProductoRequest request) {
        ProductoResponse creado = productoService.crear(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(creado.id()).toUri();
        return ResponseEntity.created(location).body(creado);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener un producto por id")
    @ApiResponse(responseCode = "404", description = "El producto no existe")
    public ProductoResponse obtener(@PathVariable Long id) {
        return productoService.obtener(id);
    }

    @GetMapping
    @Operation(summary = "Listar productos con filtros, orden y paginación",
               description = "Todos los filtros son opcionales y se combinan con AND. Tamaño máximo de página: 100.")
    @ApiResponse(responseCode = "400", description = "Orden, dirección o rango de precios inválidos")
    public PagedModel<ProductoResponse> listar(
            @Parameter(description = "Texto a buscar en nombre o SKU") @RequestParam(required = false) String q,
            @Parameter(description = "true = productos activos; false = desactivados")
            @RequestParam(defaultValue = "true") boolean activo,
            @Parameter(description = "true = solo con stock bajo; false = solo con stock suficiente")
            @RequestParam(required = false) Boolean stockBajo,
            @Parameter(description = "Precio mínimo (inclusive)") @RequestParam(required = false) BigDecimal precioMin,
            @Parameter(description = "Precio máximo (inclusive)") @RequestParam(required = false) BigDecimal precioMax,
            @Parameter(description = "Campo de orden: nombre, sku, precio, stock o creadoEn")
            @RequestParam(required = false) String sort,
            @Parameter(description = "Dirección: asc o desc") @RequestParam(required = false) String dir,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        var pageable = Paginacion.de(page, size, sort, dir, CAMPOS_ORDEN, "nombre", Sort.Direction.ASC);
        var filtro = new ProductoFiltro(q, activo, stockBajo, precioMin, precioMax);
        return new PagedModel<>(productoService.listar(filtro, pageable));
    }

    @GetMapping("/stock-bajo")
    @Operation(summary = "Productos activos que llegaron a su stock mínimo")
    public PagedModel<ProductoResponse> stockBajo(@RequestParam(defaultValue = "0") int page,
                                                  @RequestParam(defaultValue = "20") int size) {
        var pageable = Paginacion.de(page, size, null, null, CAMPOS_ORDEN, "nombre", Sort.Direction.ASC);
        return new PagedModel<>(productoService.listarStockBajo(pageable));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar nombre, precio y stock mínimo",
               description = "Solo ADMIN. El SKU y el stock no se pueden editar por aquí.")
    public ProductoResponse actualizar(@PathVariable Long id,
                                       @Valid @RequestBody ProductoActualizacion datos) {
        return productoService.actualizar(id, datos);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Desactivar un producto",
               description = "Solo ADMIN. Es un borrado lógico: el historial de movimientos se conserva.")
    public void desactivar(@PathVariable Long id) {
        productoService.desactivar(id);
    }
}