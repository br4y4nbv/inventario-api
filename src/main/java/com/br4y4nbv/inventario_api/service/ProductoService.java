package com.br4y4nbv.inventario_api.service;

import com.br4y4nbv.inventario_api.dto.ProductoActualizacion;
import com.br4y4nbv.inventario_api.dto.ProductoRequest;
import com.br4y4nbv.inventario_api.dto.ProductoResponse;
import com.br4y4nbv.inventario_api.exception.ConflictoException;
import com.br4y4nbv.inventario_api.exception.RecursoNoEncontradoException;
import com.br4y4nbv.inventario_api.model.Producto;
import com.br4y4nbv.inventario_api.repository.ProductoRepository;
import com.br4y4nbv.inventario_api.repository.ProductoSpecs;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class ProductoService {

    private final ProductoRepository productoRepository;

    @Transactional
    public ProductoResponse crear(ProductoRequest request) {
        // El SKU se normaliza para que "tec-001" y "TEC-001" sean el mismo producto
        String sku = request.sku().trim().toUpperCase(Locale.ROOT);
        if (productoRepository.existsBySku(sku)) {
            throw new ConflictoException("Ya existe un producto con el SKU " + sku);
        }

        // El stock inicial es 0: solo cambia mediante movimientos
        Producto producto = Producto.builder()
                .sku(sku)
                .nombre(request.nombre().trim())
                .precio(request.precio())
                .stockMinimo(request.stockMinimo())
                .build();
        return ProductoResponse.de(productoRepository.save(producto));
    }

    @Transactional(readOnly = true)
    public ProductoResponse obtener(Long id) {
        return ProductoResponse.de(buscar(id));
    }

    @Transactional(readOnly = true)
    public Page<ProductoResponse> listar(String q, boolean activo, Pageable pageable) {
        return productoRepository.findAll(ProductoSpecs.filtrar(q, activo), pageable)
                .map(ProductoResponse::de);
    }

    @Transactional
    public ProductoResponse actualizar(Long id, ProductoActualizacion datos) {
        Producto producto = buscar(id);
        producto.setNombre(datos.nombre().trim());
        producto.setPrecio(datos.precio());
        producto.setStockMinimo(datos.stockMinimo());
        return ProductoResponse.de(producto);
    }

    @Transactional
    public void desactivar(Long id) {
        buscar(id).setActivo(false);
    }

    private Producto buscar(Long id) {
        return productoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Producto " + id + " no encontrado"));
    }

    @Transactional(readOnly = true)
    public Page<ProductoResponse> listarStockBajo(Pageable pageable) {
        return productoRepository.findStockBajo(pageable).map(ProductoResponse::de);
    }
}