package com.preguntasSimulator.preguntas.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * Genera y valida los JWT que identifican al usuario en cada peticion.
 * El token viaja en la cabecera "Authorization: Bearer &lt;token&gt;" y
 * contiene el username como "subject" — de ahi es de donde cada endpoint
 * sabe que usuario esta haciendo la peticion, sin tener que mandarlo
 * explicitamente en cada request.
 */
@Service
public class JwtService {

    // Clave de firma HS256 (debe tener al menos 256 bits). Idealmente esto
    // se mueve a una variable de entorno (JWT_SECRET) en Render en vez de
    // quedar en el codigo, igual que se comento antes con la contraseña de
    // la BD y la api key de Google STT.
    @Value("${jwt.secret:una-clave-secreta-de-al-menos-32-caracteres-para-firmar-los-jwt-1234567890}")
    private String secret;

    // 24 horas. Pasado este tiempo el usuario debe volver a iniciar sesion.
    private static final long EXPIRACION_MS = 24 * 60 * 60 * 1000;

    private SecretKey obtenerClave() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generarToken(String username) {
        Date ahora = new Date();
        Date expiracion = new Date(ahora.getTime() + EXPIRACION_MS);

        return Jwts.builder()
                .subject(username)
                .issuedAt(ahora)
                .expiration(expiracion)
                .signWith(obtenerClave())
                .compact();
    }

    /** @return el username contenido en el token. */
    public String extraerUsername(String token) {
        return parsearClaims(token).getSubject();
    }

    public boolean esTokenValido(String token, String username) {
        try {
            Claims claims = parsearClaims(token);
            boolean noExpirado = claims.getExpiration().after(new Date());
            return claims.getSubject().equals(username) && noExpirado;
        } catch (Exception e) {
            return false;
        }
    }

    private Claims parsearClaims(String token) {
        return Jwts.parser()
                .verifyWith(obtenerClave())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
