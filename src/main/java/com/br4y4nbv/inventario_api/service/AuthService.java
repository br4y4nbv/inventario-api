package com.br4y4nbv.inventario_api.service;

import com.br4y4nbv.inventario_api.dto.LoginRequest;
import com.br4y4nbv.inventario_api.dto.RegistroRequest;
import com.br4y4nbv.inventario_api.dto.TokenResponse;
import com.br4y4nbv.inventario_api.dto.UsuarioResponse;
import com.br4y4nbv.inventario_api.exception.ConflictoException;
import com.br4y4nbv.inventario_api.model.Rol;
import com.br4y4nbv.inventario_api.model.Usuario;
import com.br4y4nbv.inventario_api.repository.UsuarioRepository;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Optional;

@Service
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;

    // Hash válido de una contraseña cualquiera: se usa cuando el correo no existe (ver login)
    private final String hashFalso;

    public AuthService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder,
                       TokenService tokenService) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.hashFalso = passwordEncoder.encode("contraseña-inexistente");
    }

    @Transactional
    public UsuarioResponse registrar(RegistroRequest request) {
        String email = normalizar(request.email());
        if (usuarioRepository.existsByEmail(email)) {
            throw new ConflictoException("Ya existe una cuenta con ese correo");
        }
        Usuario usuario = usuarioRepository.save(Usuario.builder()
                .nombre(request.nombre().trim())
                .email(email)
                .password(passwordEncoder.encode(request.password()))   // BCrypt, nunca texto plano
                .rol(Rol.USER)                                          // el rol nunca lo decide el cliente
                .build());
        return UsuarioResponse.de(usuario);
    }

    @Transactional(readOnly = true)
    public TokenResponse login(LoginRequest request) {
        Optional<Usuario> usuario = usuarioRepository.findByEmail(normalizar(request.email()));

        // Se compara la contraseña SIEMPRE, incluso si el correo no existe. Si se saltara la
        // comparación, responder más rápido delataría qué correos están registrados (ataque por tiempo).
        String hash = usuario.map(Usuario::getPassword).orElse(hashFalso);
        boolean coincide = passwordEncoder.matches(request.password(), hash);

        // Mismo mensaje para "correo inexistente" y "contraseña incorrecta"
        if (usuario.isEmpty() || !coincide) {
            throw new BadCredentialsException("Credenciales inválidas");
        }
        return tokenService.emitir(usuario.get());
    }

    private String normalizar(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}