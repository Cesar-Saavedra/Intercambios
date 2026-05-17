package cl.duoc.ms_intercambio.security;

import javax.crypto.SecretKey;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    private SecretKey getKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    public Claims extraerClaims(String token) {
        return Jwts.parser().verifyWith(getKey()).build().parseClaimsJws(token).getPayload();
    }

    public boolean esTokenValido(String token) {
        try { extraerClaims(token); return true; } catch (Exception e) { return false; }
    }

    public Integer extraerId(String token) {
        return extraerClaims(token).get("id", Integer.class);
    }

    public String extraerNombre(String token) {
        return extraerClaims(token).get("nombre", String.class);
    }

    public String extraerRol(String token) {
        return extraerClaims(token).get("rol", String.class);
    }

    public String obtenerTokenDelHeader(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) return authHeader.substring(7);
        return null;
    }
}
