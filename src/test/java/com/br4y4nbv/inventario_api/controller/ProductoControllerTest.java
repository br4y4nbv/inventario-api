package com.br4y4nbv.inventario_api.controller;

import com.br4y4nbv.inventario_api.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.test.context.support.WithMockUser;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@WithMockUser(username = "admin@test.com", roles = "ADMIN")
@Transactional
class ProductoControllerTest {

    @Autowired MockMvc mockMvc;

    private static String json(String sku, String nombre) {
        return """
                {"sku":"%s","nombre":"%s","precio":89900.00,"stockMinimo":5}
                """.formatted(sku, nombre);
    }

    // Crea un producto y devuelve la URL de su cabecera Location
    private String crear(String sku, String nombre) throws Exception {
        return mockMvc.perform(post("/api/productos")
                        .contentType(MediaType.APPLICATION_JSON).content(json(sku, nombre)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getHeader("Location");
    }

    private String crearConPrecio(String sku, String nombre, String precio) throws Exception {
        return mockMvc.perform(post("/api/productos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"sku":"%s","nombre":"%s","precio":%s,"stockMinimo":5}
                                """.formatted(sku, nombre, precio)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getHeader("Location");
    }

    @Test
    void creaProductoConStockCeroYSkuNormalizado() throws Exception {
        mockMvc.perform(post("/api/productos")
                        .contentType(MediaType.APPLICATION_JSON).content(json("tec-001", "Teclado")))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.sku").value("TEC-001"))
                .andExpect(jsonPath("$.stock").value(0))
                .andExpect(jsonPath("$.stockBajo").value(true))
                .andExpect(jsonPath("$.activo").value(true));
    }

    @Test
    void skuDuplicadoDevuelve409() throws Exception {
        crear("DUP-001", "Mouse");

        mockMvc.perform(post("/api/productos")
                        .contentType(MediaType.APPLICATION_JSON).content(json("dup-001", "Otro mouse")))
                .andExpect(status().isConflict());
    }

    @Test
    void datosInvalidosDevuelve400ConDetallePorCampo() throws Exception {
        mockMvc.perform(post("/api/productos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"sku":"","nombre":"","precio":-5,"stockMinimo":-1}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.sku").exists())
                .andExpect(jsonPath("$.errores.nombre").exists())
                .andExpect(jsonPath("$.errores.precio").exists())
                .andExpect(jsonPath("$.errores.stockMinimo").exists());
    }

    @Test
    void jsonMalFormadoDevuelve400() throws Exception {
        mockMvc.perform(post("/api/productos")
                        .contentType(MediaType.APPLICATION_JSON).content("{ \"sku\": "))
                .andExpect(status().isBadRequest());
    }

    @Test
    void productoInexistenteDevuelve404() throws Exception {
        mockMvc.perform(get("/api/productos/999999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void actualizaNombrePrecioYStockMinimo() throws Exception {
        String url = crear("UPD-001", "Monitor");

        mockMvc.perform(put(url).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"Monitor 24 pulgadas","precio":650000.00,"stockMinimo":2}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Monitor 24 pulgadas"))
                .andExpect(jsonPath("$.sku").value("UPD-001"))
                .andExpect(jsonPath("$.stockMinimo").value(2));
    }

    @Test
    void listaConBusquedaPorTextoYPaginacion() throws Exception {
        crear("LST-001", "Teclado mecánico");
        crear("LST-002", "Mouse inalámbrico");

        mockMvc.perform(get("/api/productos").param("q", "teclado"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].sku").value("LST-001"));
    }

    @Test
    void desactivarOcultaElProductoDelListadoPeroConservaElRegistro() throws Exception {
        String url = crear("DEL-001", "Cable HDMI");

        mockMvc.perform(delete(url)).andExpect(status().isNoContent());

        mockMvc.perform(get("/api/productos").param("q", "DEL-001"))
                .andExpect(jsonPath("$.page.totalElements").value(0));
        mockMvc.perform(get(url))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activo").value(false));
    }

    @Test
    void filtraPorRangoDePrecio() throws Exception {
        crearConPrecio("PRE-001", "Barato", "1000.00");
        crearConPrecio("PRE-002", "Medio", "5000.00");
        crearConPrecio("PRE-003", "Caro", "9000.00");

        mockMvc.perform(get("/api/productos").param("precioMin", "2000").param("precioMax", "6000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].sku").value("PRE-002"));
    }

    @Test
    void ordenaPorPrecioDescendente() throws Exception {
        crearConPrecio("ORD-001", "Barato", "1000.00");
        crearConPrecio("ORD-002", "Medio", "5000.00");
        crearConPrecio("ORD-003", "Caro", "9000.00");

        mockMvc.perform(get("/api/productos").param("sort", "precio").param("dir", "desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].sku").value("ORD-003"))
                .andExpect(jsonPath("$.content[2].sku").value("ORD-001"));
    }

    @Test
    void camposDeOrdenFueraDeLaListaBlancaDevuelven400() throws Exception {
        mockMvc.perform(get("/api/productos").param("sort", "version"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/productos").param("sort", "nombre").param("dir", "sideways"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rangoDePreciosInvertidoDevuelve400() throws Exception {
        mockMvc.perform(get("/api/productos").param("precioMin", "9000").param("precioMax", "1000"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void filtraPorStockBajo() throws Exception {
        crear("SBJ-001", "Producto sin stock");   // stock 0 <= mínimo 5

        mockMvc.perform(get("/api/productos").param("stockBajo", "true").param("q", "SBJ-001"))
                .andExpect(jsonPath("$.page.totalElements").value(1));
        mockMvc.perform(get("/api/productos").param("stockBajo", "false").param("q", "SBJ-001"))
                .andExpect(jsonPath("$.page.totalElements").value(0));
    }

    @Test
    void elTamanoDePaginaTieneTope() throws Exception {
        mockMvc.perform(get("/api/productos").param("size", "100000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.size").value(100));
    }
}