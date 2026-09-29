package cn.campus.treehole.web;

import cn.campus.treehole.domain.User;
import cn.campus.treehole.repo.UserRepository;
import cn.campus.treehole.security.JwtService;
import cn.campus.treehole.service.CurrentUserService;
import cn.campus.treehole.service.AccountTokenService;
import cn.campus.treehole.web.ApiErrorHandler.ApiException;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.time.Duration;
import java.util.*;
import static cn.campus.treehole.domain.Enums.*;

@RestController @RequestMapping("/api/v1/auth") @RequiredArgsConstructor
public class AuthController {
  private final UserRepository users; private final PasswordEncoder encoder; private final JwtService jwt; private final CurrentUserService current; private final AccountTokenService tokens;
  public record RegisterRequest(@Email String email,@Size(min=8,max=72) String password,@NotBlank @Size(max=50) String name){}
  public record LoginRequest(@Email String email,@NotBlank String password){}
  public record EmailRequest(@Email String email){}
  public record ResetRequest(@NotBlank String token,@Size(min=8,max=72) String password){}
  public record UserView(UUID id,String email,String name,Role role){}
  @PostMapping("/register") public UserView register(@Valid @RequestBody RegisterRequest req){
    if(users.findByEmailIgnoreCase(req.email()).isPresent()) throw new ApiException(HttpStatus.CONFLICT,"邮箱已注册");
    var u=new User(); u.setEmail(req.email().toLowerCase()); u.setPasswordHash(encoder.encode(req.password())); u.setDisplayName(req.name()); u.setRole(Role.STUDENT); u.setStatus(UserStatus.UNVERIFIED); users.save(u); tokens.sendVerification(u); return view(u);
  }
  @PostMapping("/login") public UserView login(@Valid @RequestBody LoginRequest req,HttpServletResponse response){
    var u=users.findByEmailIgnoreCase(req.email()).orElseThrow(()->new ApiException(HttpStatus.UNAUTHORIZED,"邮箱或密码错误"));
    if(u.getStatus()!=UserStatus.ACTIVE||!encoder.matches(req.password(),u.getPasswordHash())) throw new ApiException(HttpStatus.UNAUTHORIZED,"邮箱或密码错误");
    var cookie=ResponseCookie.from("treehole_token",jwt.create(u.getEmail(),u.getRole().name())).httpOnly(true).sameSite("Lax").path("/").maxAge(Duration.ofHours(8)).build();
    response.addHeader(HttpHeaders.SET_COOKIE,cookie.toString()); return view(u);
  }
  @PostMapping("/logout") public void logout(HttpServletResponse response){ response.addHeader(HttpHeaders.SET_COOKIE,ResponseCookie.from("treehole_token","").httpOnly(true).path("/").maxAge(0).build().toString()); }
  @PostMapping("/verify") @Transactional public void verify(@RequestParam String token){ UUID userId=tokens.consume(token,"VERIFY_EMAIL"); var u=users.findById(userId).orElseThrow(); u.setStatus(UserStatus.ACTIVE); }
  @PostMapping("/forgot-password") public void forgot(@Valid @RequestBody EmailRequest req){ users.findByEmailIgnoreCase(req.email()).ifPresent(tokens::sendReset); }
  @PostMapping("/reset-password") public void reset(@Valid @RequestBody ResetRequest req){ UUID userId=tokens.consume(req.token(),"RESET_PASSWORD"); tokens.resetPassword(userId,req.password()); }
  @GetMapping("/me") public UserView me(){ return view(current.get()); }
  private UserView view(User u){ return new UserView(u.getId(),u.getEmail(),u.getDisplayName(),u.getRole()); }
}
