package com.br4y4nbv.inventario_api.config;

import com.br4y4nbv.inventario_api.model.Rol;
import com.br4y4nbv.inventario_api.model.Usuario;
import com.br4y4nbv.inventario_api.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Locale;

// Crea el primer ADMIN al arrancar, solo si ADMIN_EMAIL y ADMIN_PASSWORD están definidas.
// Ninguna credencial de administrador vive en el código ni en el repositorio de producción.
@Component
public class AdminInicial implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminInicial.class);

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final String email;
    private final String password;

    public AdminInicial(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder,
                        @Value("${app.admin.email:}") String email,
                        @Value("${app.admin.password:}") String password) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.email = email;
        this.password = password;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (email.isBlank() || password.isBlank()) return;
        if (password.length() < 8) {
            throw new IllegalStateException("ADMIN_PASSWORD debe tener al menos 8 caracteres");
        }

        String emailNormalizado = email.trim().toLowerCase(Locale.ROOT);
        if (usuarioRepository.existsByEmail(emailNormalizado)) return;

        usuarioRepository.save(Usuario.builder()
                .nombre("Administrador")
                .email(emailNormalizado)
                .password(passwordEncoder.encode(password))
                .rol(Rol.ADMIN)
                .build());
        log.info("Administrador inicial creado: {}", emailNormalizado);   // la contraseña nunca se registra
    }
}