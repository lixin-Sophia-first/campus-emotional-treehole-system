package cn.campus.treehole.service;

import cn.campus.treehole.domain.User;
import cn.campus.treehole.web.ApiErrorHandler.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;

@Service @RequiredArgsConstructor
public class AccountTokenService {
  private final JdbcTemplate jdbc; private final JavaMailSender mail; private final PasswordEncoder encoder;
  @Value("${app.frontend-url}") private String frontendUrl;
  @Transactional public void sendVerification(User user){ String token=create(user,"VERIFY_EMAIL",Duration.ofHours(24)); send(user.getEmail(),"Verify your Tree Hole account","Verify email",frontendUrl+"/verify?token="+token,"This link is valid for 24 hours."); }
  @Transactional public void sendReset(User user){ String token=create(user,"RESET_PASSWORD",Duration.ofMinutes(30)); send(user.getEmail(),"Reset your Tree Hole password","Reset password",frontendUrl+"/reset-password?token="+token,"This link is valid for 30 minutes."); }
  @Transactional public UUID consume(String raw,String type){ var rows=jdbc.queryForList("select id,user_id from account_tokens where token_hash=? and token_type=? and used_at is null and expires_at>now()",hash(raw),type); if(rows.isEmpty()) throw new ApiException(HttpStatus.BAD_REQUEST,"The link is invalid or has expired"); UUID id=(UUID)rows.getFirst().get("id"); UUID user=(UUID)rows.getFirst().get("user_id"); jdbc.update("update account_tokens set used_at=now() where id=?",id); return user; }
  @Transactional public void resetPassword(UUID userId,String password){ jdbc.update("update users set password_hash=? where id=?",encoder.encode(password),userId); jdbc.update("update account_tokens set used_at=now() where user_id=? and token_type='RESET_PASSWORD' and used_at is null",userId); }
  private String create(User user,String type,Duration ttl){ String token=UUID.randomUUID()+"."+UUID.randomUUID(); jdbc.update("delete from account_tokens where user_id=? and token_type=? and used_at is null",user.getId(),type); jdbc.update("insert into account_tokens(user_id,token_hash,token_type,expires_at) values (?,?,?,?)",user.getId(),hash(token),type,Timestamp.from(Instant.now().plus(ttl))); return token; }
  private String hash(String raw){ try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8)));}catch(Exception e){throw new IllegalStateException(e);} }
  private void send(String to,String subject,String action,String url,String note){ mail.send(mime->{ var helper=new MimeMessageHelper(mime,true,"UTF-8"); helper.setFrom("noreply@treehole.local"); helper.setTo(to); helper.setSubject(subject); helper.setText(action+": "+url+"\n\n"+note,"<div style='font-family:Arial,sans-serif;line-height:1.6;color:#173d35'><h2>Tree Hole</h2><p>Use the button below to continue.</p><p><a href='"+url+"' style='display:inline-block;padding:12px 18px;background:#1f806d;color:white;text-decoration:none;border-radius:10px'>"+action+"</a></p><p>"+note+"</p></div>"); }); }
}
