package com.br4y4nbv.inventario_api.controller;

import com.br4y4nbv.inventario_api.TestcontainersConfiguration;
import com.br4y4nbv.inventario_api.model.Rol;
import com.br4y4nbv.inventario_api.model.Usuario;
import com.br4y4nbv.inventario_api.repository.UsuarioRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Transactional
class SeguridadApiTest {

    private static final String CLAVE = "clave12345";
    private static final String PRODUCTO = """
            {"sku":"SEG-001","nombre":"Producto seguro","precio":1000.00,"stockMinimo":1}
            """;

    @Autowired MockMvc mockMvc;
    @Autowired UsuarioRepository usuarioRepository;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired JwtEncoder jwtEncoder;

    // ---------- Ayudas ----------

    private void registrar(String email) throws Exception {
        mockMvc.perform(post("/api/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"Usuario Prueba","email":"%s","password":"%s"}
                                """.formatted(email, CLAVE)))
                .andExpect(status().isCreated());
    }

    private String login(String email) throws Exception {
        String cuerpo = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"%s"}
                                """.formatted(email, CLAVE)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(cuerpo, "$.accessToken");
    }

    private String tokenUser(String email) throws Exception {
        registrar(email);
        return login(email);
    }

    private String tokenAdmin() throws Exception {
        // Un ADMIN no se puede crear por la API: se inserta directamente, como hace AdminInicial
        usuarioRepository.save(Usuario.builder().nombre("Admin").email("admin@test.com")
                .password(passwordEncoder.encode(CLAVE)).rol(Rol.ADMIN).build());
        return login("admin@test.com");
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }

    // ---------- Registro y login ----------

    @Test
    void registroCreaUsuarioConRolUserNormalizaElCorreoYNoExponeLaPassword() throws Exception {
        mockMvc.perform(post("/api/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"Ana","email":"Ana@Test.COM","password":"clave12345"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("ana@test.com"))
                .andExpect(jsonPath("$.rol").value("USER"))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void registroConCorreoDuplicadoDevuelve409() throws Exception {
        registrar("dup@test.com");

        mockMvc.perform(post("/api/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"Otra","email":"DUP@test.com","password":"clave12345"}
                                """))
                .andExpect(status().isConflict());
    }

    @Test
    void registroConDatosInvalidosDevuelve400() throws Exception {
        mockMvc.perform(post("/api/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"","email":"no-es-correo","password":"123"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.nombre").exists())
                .andExpect(jsonPath("$.errores.email").exists())
                .andExpect(jsonPath("$.errores.password").exists());
    }

    @Test
    void loginCorrectoDevuelveUnTokenBearer() throws Exception {
        registrar("login@test.com");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"login@test.com","password":"clave12345"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"));
    }

    @Test
    void loginConContrasenaIncorrectaOCorreoInexistenteDevuelve401() throws Exception {
        registrar("falla@test.com");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"falla@test.com","password":"incorrecta1"}
                                """))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"noexiste@test.com","password":"clave12345"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    // ---------- Tokens ----------

    @Test
    void sinTokenDevuelve401() throws Exception {
        mockMvc.perform(get("/api/productos")).andExpect(status().isUnauthorized());
    }

    @Test
    void tokenInvalidoDevuelve401() throws Exception {
        mockMvc.perform(get("/api/productos")
                        .header(HttpHeaders.AUTHORIZATION, bearer("esto.no.es-un-token")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenExpiradoDevuelve401() throws Exception {
        Instant hace = Instant.now().minus(Duration.ofHours(2));
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("inventario-api").subject("viejo@test.com")
                .issuedAt(hace.minusSeconds(3600)).expiresAt(hace)
                .claim("rol", "ADMIN").build();
        String vencido = jwtEncoder.encode(
                JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims)
        ).getTokenValue();

        mockMvc.perform(get("/api/productos").header(HttpHeaders.AUTHORIZATION, bearer(vencido)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void yoDevuelveElUsuarioDelToken() throws Exception {
        String token = tokenUser("yo@test.com");

        mockMvc.perform(get("/api/auth/yo").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("yo@test.com"))
                .andExpect(jsonPath("$.rol").value("USER"));
    }

    // ---------- Roles ----------

    @Test
    void userPuedeConsultarPeroNoCrearProductos() throws Exception {
        String token = tokenUser("user1@test.com");

        mockMvc.perform(get("/api/productos").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/productos")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content(PRODUCTO))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminPuedeCrearProductos() throws Exception {
        mockMvc.perform(post("/api/productos")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenAdmin()))
                        .contentType(MediaType.APPLICATION_JSON).content(PRODUCTO))
                .andExpect(status().isCreated());
    }

    @Test
    void userPuedeMoverStockPeroNoDesactivarProductos() throws Exception {
        String url = mockMvc.perform(post("/api/productos")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenAdmin()))
                        .contentType(MediaType.APPLICATION_JSON).content(PRODUCTO))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getHeader("Location");

        String token = tokenUser("user2@test.com");

        // El movimiento queda atribuido al usuario del token
        mockMvc.perform(post(url + "/movimientos")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tipo":"ENTRADA","cantidad":5,"motivo":"compra"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.registradoPor").value("Usuario Prueba"));

        mockMvc.perform(delete(url).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isForbidden());
    }
}