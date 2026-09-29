package cn.campus.treehole.config;

import cn.campus.treehole.domain.*;
import cn.campus.treehole.repo.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import static cn.campus.treehole.domain.Enums.*;

@Configuration @RequiredArgsConstructor
public class SeedConfig {
  private final UserRepository users; private final PasswordEncoder encoder;
  @Bean CommandLineRunner seedAdmin(@Value("${app.admin-email}") String email,@Value("${app.admin-password}") String password){
    return args->{ if(!users.existsByRole(Role.ADMIN)) { var u=new User(); u.setEmail(email.toLowerCase()); u.setPasswordHash(encoder.encode(password)); u.setDisplayName("System Administrator"); u.setRole(Role.ADMIN); users.save(u); } };
  }
}
