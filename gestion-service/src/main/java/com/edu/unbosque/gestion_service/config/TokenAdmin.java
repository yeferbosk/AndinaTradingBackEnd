package com.edu.unbosque.gestion_service.config;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import javax.crypto.SecretKey;

@Component
public class TokenAdmin {

    @Value("${jwt.secret}")
    private String llave;


    /**
     *
     * Genera el token que funcionara como la sesion de usuario
     *
     * @param identificadorUsuario
     * @return Genera un token
     */
    public String generarToken(String identificadorUsuario) {
        SecretKey key = Keys.hmacShaKeyFor(llave.getBytes());
        return Jwts.builder()
                .setSubject(identificadorUsuario)
                .signWith(key)
                .compact();
    }

    /**
     *
     * Esto valida el token creado y devuelve el id del usuario para poder ser buscado
     *
     * @Author Andres Cuta
     * @param token
     * @return
     */
    public String validarTokenIdentificadorUsuario(String token) {
        try {
            SecretKey key = Keys.hmacShaKeyFor(llave.getBytes());
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            return claims.getSubject();
        } catch (Exception e) {
            return null;
        }
    }

}