package com.br4y4nbv.inventario_api.controller;

import com.br4y4nbv.inventario_api.dto.MovimientoFiltro;
import com.br4y4nbv.inventario_api.dto.MovimientoRequest;
import com.br4y4nbv.inventario_api.dto.MovimientoResponse;
import com.br4y4nbv.inventario_api.model.TipoMovimiento;
import com.br4y4nbv.inventario_api.service.MovimientoService;
import com.br4y4nbv.inventario_api.util.Paginacion;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PagedModel;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Set;

@RestController
@RequestMapping("/api/productos/{productoId}/movimientos")
@RequiredArgsConstructor
@Tag(name = "Movimientos", description = "Entradas, salidas y ajustes de stock. USER o ADMIN.")
public class MovimientoController {

    private static final Set<String> CAMPOS_ORDEN = Set.of("fecha", "cantidad", "tipo");

    private final MovimientoService movimientoService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Registrar un movimiento de stock",
               description = """
                       ENTRADA suma y SALIDA resta (cantidad positiva). AJUSTE suma una cantidad con signo \
                       (por ejemplo -3 para una merma). El movimiento queda atribuido al usuario del token.""")
    @ApiResponse(responseCode = "409", description = "Stock insuficiente, producto inactivo o conflicto de concurrencia")
    public MovimientoResponse registrar(@PathVariable Long productoId,
                                        @Valid @RequestBody MovimientoRequest request) {
        return movimientoService.registrar(productoId, request);
    }

    @GetMapping
    @Operation(summary = "Historial de movimientos de un producto",
               description = "Más recientes primero por defecto. Las fechas se interpretan en UTC.")
    @ApiResponse(responseCode = "400", description = "Tipo, orden o rango de fechas inválidos")
    public PagedModel<MovimientoResponse> historial(
            @PathVariable Long productoId,
            @Parameter(description = "ENTRADA, SALIDA o AJUSTE") @RequestParam(required = false) TipoMovimiento tipo,
            @Parameter(description = "Desde esta fecha (inclusive), formato AAAA-MM-DD")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @Parameter(description = "Hasta esta fecha (inclusive), formato AAAA-MM-DD")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @Parameter(description = "Campo de orden: fecha, cantidad o tipo") @RequestParam(required = false) String sort,
            @Parameter(description = "Dirección: asc o desc") @RequestParam(required = false) String dir,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        var pageable = Paginacion.de(page, size, sort, dir, CAMPOS_ORDEN, "fecha", Sort.Direction.DESC);
        return new PagedModel<>(movimientoService.historial(
                productoId, new MovimientoFiltro(tipo, desde, hasta), pageable));
    }
}