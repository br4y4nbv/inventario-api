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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Transactional
class MovimientoControllerTest {

    @Autowired MockMvc mockMvc;

    // Crea un producto (stock 0, mínimo 5) y devuelve su URL
    private String crearProducto(String sku) throws Exception {
        return mockMvc.perform(post("/api/productos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"sku":"%s","nombre":"Producto %s","precio":1000.00,"stockMinimo":5}
                                """.formatted(sku, sku)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getHeader("Location");
    }

    private org.springframework.test.web.servlet.ResultActions mover(
            String urlProducto, String tipo, int cantidad) throws Exception {
        return mockMvc.perform(post(urlProducto + "/movimientos")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"tipo":"%s","cantidad":%d,"motivo":"prueba"}
                        """.formatted(tipo, cantidad)));
    }

    @Test
    void entradaAumentaElStock() throws Exception {
        String url = crearProducto("MOV-001");

        mover(url, "ENTRADA", 10)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.stockResultante").value(10))
                .andExpect(jsonPath("$.tipo").value("ENTRADA"));

        mockMvc.perform(get(url))
                .andExpect(jsonPath("$.stock").value(10))
                .andExpect(jsonPath("$.stockBajo").value(false));
    }

    @Test
    void salidaDescuentaElStock() throws Exception {
        String url = crearProducto("MOV-002");
        mover(url, "ENTRADA", 10).andExpect(status().isCreated());

        mover(url, "SALIDA", 4)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.stockResultante").value(6));
    }

    @Test
    void salidaMayorAlStockDevuelve409YNoCambiaNada() throws Exception {
        String url = crearProducto("MOV-003");
        mover(url, "ENTRADA", 3).andExpect(status().isCreated());

        mover(url, "SALIDA", 5).andExpect(status().isConflict());

        mockMvc.perform(get(url)).andExpect(jsonPath("$.stock").value(3));
    }

    @Test
    void ajusteNegativoRestaYAjusteCeroSeRechaza() throws Exception {
        String url = crearProducto("MOV-004");
        mover(url, "ENTRADA", 10).andExpect(status().isCreated());

        mover(url, "AJUSTE", -3)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.stockResultante").value(7));

        mover(url, "AJUSTE", 0).andExpect(status().isBadRequest());
    }

    @Test
    void entradaConCantidadCeroDevuelve400() throws Exception {
        String url = crearProducto("MOV-005");

        mover(url, "ENTRADA", 0)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.cantidadCoherente").exists());
    }

    @Test
    void tipoInexistenteDevuelve400() throws Exception {
        String url = crearProducto("MOV-006");

        mockMvc.perform(post(url + "/movimientos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tipo":"VENTA","cantidad":1}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void productoInexistenteDevuelve404() throws Exception {
        mover("/api/productos/999999", "ENTRADA", 1).andExpect(status().isNotFound());
    }

    @Test
    void productoInactivoNoAdmiteMovimientos() throws Exception {
        String url = crearProducto("MOV-007");
        mockMvc.perform(delete(url)).andExpect(status().isNoContent());

        mover(url, "ENTRADA", 1).andExpect(status().isConflict());
    }

    @Test
    void historialMuestraPrimeroElMasReciente() throws Exception {
        String url = crearProducto("MOV-008");
        mover(url, "ENTRADA", 10).andExpect(status().isCreated());
        mover(url, "SALIDA", 4).andExpect(status().isCreated());

        mockMvc.perform(get(url + "/movimientos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements").value(2))
                .andExpect(jsonPath("$.content[0].tipo").value("SALIDA"))
                .andExpect(jsonPath("$.content[1].tipo").value("ENTRADA"));
    }

    @Test
    void stockBajoListaSoloLosProductosQueLlegaronAlMinimo() throws Exception {
        String url = crearProducto("LOW-001");

        // Stock 0 <= mínimo 5: aparece
        mockMvc.perform(get("/api/productos/stock-bajo"))
                .andExpect(jsonPath("$.content[?(@.sku=='LOW-001')]").exists());

        mover(url, "ENTRADA", 10).andExpect(status().isCreated());

        // Stock 10 > mínimo 5: ya no aparece
        mockMvc.perform(get("/api/productos/stock-bajo"))
                .andExpect(jsonPath("$.content[?(@.sku=='LOW-001')]").doesNotExist());
    }
}