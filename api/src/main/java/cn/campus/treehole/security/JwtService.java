package cn.campus.treehole.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.Date;

@Service
public class JwtService {
  private final SecretKey key;
  public JwtService(@Value("${app.jwt-secret}") String secret) {
    this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
  }
  public String create(String email, String role) {
    Instant now=Instant.now();
    return Jwts.builder().subject(email).claim("role", role).issuedAt(Date.from(now))
      .expiration(Date.from(now.plus(Duration.ofHours(8)))).signWith(key).compact();
  }
  public Claims parse(String token) { return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload(); }
}

