package cn.campus.treehole.security;

import cn.campus.treehole.domain.User;
import cn.campus.treehole.repo.UserRepository;
import cn.campus.treehole.web.ApiErrorHandler.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;

@Service @RequiredArgsConstructor
public class RefreshTokenService {
  public record Issued(String raw,Instant expiresAt){}
  private final JdbcTemplate jdbc; private final UserRepository users;
  private final SecureRandom random=new SecureRandom();

  @Transactional public Issued issue(User user){ return insert(user,null); }
  @Transactional public UserAndToken rotate(String raw){
    var rows=jdbc.queryForList("select id,user_id from refresh_tokens where token_hash=? and revoked_at is null and expires_at>now() for update",hash(raw));
    if(rows.isEmpty()) throw new ApiException(HttpStatus.UNAUTHORIZED,"Your session has expired. Please sign in again");
    UUID oldId=(UUID)rows.getFirst().get("id"),userId=(UUID)rows.getFirst().get("user_id");
    var user=users.findById(userId).orElseThrow(()->new ApiException(HttpStatus.UNAUTHORIZED,"Account not found"));
    var next=insert(user,null); UUID replacement=jdbc.queryForObject("select id from refresh_tokens where token_hash=?",UUID.class,hash(next.raw()));
    jdbc.update("update refresh_tokens set revoked_at=now(),replaced_by=? where id=?",replacement,oldId);
    return new UserAndToken(user,next);
  }
  @Transactional public void revoke(String raw){ if(raw!=null&&!raw.isBlank()) jdbc.update("update refresh_tokens set revoked_at=coalesce(revoked_at,now()) where token_hash=?",hash(raw)); }
  @Transactional public void revokeAll(UUID userId){ jdbc.update("update refresh_tokens set revoked_at=coalesce(revoked_at,now()) where user_id=? and revoked_at is null",userId); }
  private Issued insert(User user,UUID ignored){ byte[] bytes=new byte[32]; random.nextBytes(bytes); String raw=Base64.getUrlEncoder().withoutPadding().encodeToString(bytes); Instant expiry=Instant.now().plus(Duration.ofDays(30)); jdbc.update("insert into refresh_tokens(user_id,token_hash,expires_at) values (?,?,?)",user.getId(),hash(raw),Timestamp.from(expiry)); return new Issued(raw,expiry); }
  private String hash(String raw){ try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8)));}catch(Exception e){throw new IllegalStateException(e);} }
  public record UserAndToken(User user,Issued token){}
}
