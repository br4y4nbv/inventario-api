package com.br4y4nbv.inventario_api.service;

import com.br4y4nbv.inventario_api.config.JwtProperties;
import com.br4y4nbv.inventario_api.dto.TokenResponse;
import com.br4y4nbv.inventario_api.model.Usuario;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class TokenService {

    public static final String EMISOR = "inventario-api";

    private final JwtEncoder encoder;
    private final JwtProperties props;

    public TokenResponse emitir(Usuario usuario) {
        Instant ahora = Instant.now();
        Duration duracion = Duration.ofMinutes(props.expiracionMinutos());

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(EMISOR)
                .subject(usuario.getEmail())           // identifica al usuario
                .issuedAt(ahora)
                .expiresAt(ahora.plus(duracion))       // el token caduca solo
                .claim("rol", usuario.getRol().name()) // la autorización viaja dentro del token
                .build();

        // Hay que indicar el algoritmo en la cabecera, o el encoder no sabe con qué clave firmar
        String token = encoder.encode(
                JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims)
        ).getTokenValue();

        return new TokenResponse(token, "Bearer", duracion.toSeconds());
    }
}