package com.br4y4nbv.inventario_api.service;

import com.br4y4nbv.inventario_api.dto.LoginRequest;
import com.br4y4nbv.inventario_api.dto.RegistroRequest;
import com.br4y4nbv.inventario_api.dto.TokenResponse;
import com.br4y4nbv.inventario_api.dto.UsuarioResponse;
import com.br4y4nbv.inventario_api.exception.ConflictoException;
import com.br4y4nbv.inventario_api.model.Rol;
import com.br4y4nbv.inventario_api.model.Usuario;
import com.br4y4nbv.inventario_api.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock UsuarioRepository usuarioRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock TokenService tokenService;

    AuthService servicio;

    @BeforeEach
    void preparar() {
        // El constructor de AuthService calcula un hash falso con el encoder
        when(passwordEncoder.encode("contraseña-inexistente")).thenReturn("HASH_FALSO");
        servicio = new AuthService(usuarioRepository, passwordEncoder, tokenService);
    }

    @Test
    void registrarCifraLaPasswordNormalizaElCorreoYSiempreAsignaRolUser() {
        when(usuarioRepository.existsByEmail("ana@test.com")).thenReturn(false);
        when(passwordEncoder.encode("clave12345")).thenReturn("HASH");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        UsuarioResponse r = servicio.registrar(new RegistroRequest("  Ana ", "ANA@Test.com", "clave12345"));

        assertThat(r.email()).isEqualTo("ana@test.com");
        assertThat(r.rol()).isEqualTo(Rol.USER);

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(captor.capture());
        assertThat(captor.getValue().getPassword()).isEqualTo("HASH");   // nunca en texto plano
        assertThat(captor.getValue().getNombre()).isEqualTo("Ana");
    }

    @Test
    void registrarConCorreoExistenteSeRechaza() {
        when(usuarioRepository.existsByEmail("ana@test.com")).thenReturn(true);

        assertThatThrownBy(() -> servicio.registrar(new RegistroRequest("Ana", "ana@test.com", "clave12345")))
                .isInstanceOf(ConflictoException.class);
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void loginCorrectoEmiteElToken() {
        Usuario u = Usuario.builder().email("ana@test.com").password("HASH").rol(Rol.USER).build();
        TokenResponse token = new TokenResponse("jwt", "Bearer", 3600);
        when(usuarioRepository.findByEmail("ana@test.com")).thenReturn(Optional.of(u));
        when(passwordEncoder.matches("clave12345", "HASH")).thenReturn(true);
        when(tokenService.emitir(u)).thenReturn(token);

        assertThat(servicio.login(new LoginRequest("ANA@test.com", "clave12345"))).isSameAs(token);
    }

    @Test
    void loginConContrasenaIncorrectaSeRechaza() {
        Usuario u = Usuario.builder().email("ana@test.com").password("HASH").rol(Rol.USER).build();
        when(usuarioRepository.findByEmail("ana@test.com")).thenReturn(Optional.of(u));
        when(passwordEncoder.matches("mala", "HASH")).thenReturn(false);

        assertThatThrownBy(() -> servicio.login(new LoginRequest("ana@test.com", "mala")))
                .isInstanceOf(BadCredentialsException.class);
        verifyNoInteractions(tokenService);
    }

    @Test
    void loginConCorreoInexistenteTambienCompararLaContrasena() {
        when(usuarioRepository.findByEmail("nadie@test.com")).thenReturn(Optional.empty());
        when(passwordEncoder.matches("clave12345", "HASH_FALSO")).thenReturn(false);

        assertThatThrownBy(() -> servicio.login(new LoginRequest("nadie@test.com", "clave12345")))
                .isInstanceOf(BadCredentialsException.class);

        // La comparación se ejecuta igual aunque el correo no exista: así el tiempo de respuesta
        // no delata qué correos están registrados
        verify(passwordEncoder).matches("clave12345", "HASH_FALSO");
        verifyNoInteractions(tokenService);
    }
}