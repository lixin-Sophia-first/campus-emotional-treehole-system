package cn.campus.treehole.web;

import cn.campus.treehole.domain.User;
import cn.campus.treehole.repo.UserRepository;
import cn.campus.treehole.security.JwtService;
import cn.campus.treehole.security.RefreshTokenService;
import cn.campus.treehole.security.LoginRateLimitService;
import cn.campus.treehole.service.CurrentUserService;
import cn.campus.treehole.service.AccountTokenService;
import cn.campus.treehole.web.ApiErrorHandler.ApiException;
import jakarta.servlet.http.*;
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
  private final UserRepository users; private final PasswordEncoder encoder; private final JwtService jwt; private final CurrentUserService current; private final AccountTokenService tokens; private final RefreshTokenService refreshTokens; private final LoginRateLimitService loginLimits;
  public record RegisterRequest(@Email String email,@Size(min=8,max=72) String password,@NotBlank @Size(max=50) String name){}
  public record LoginRequest(@Email String email,@NotBlank String password){}
  public record EmailRequest(@Email String email){}
  public record ResetRequest(@NotBlank String token,@Size(min=8,max=72) String password){}
  public record UserView(UUID id,String email,String name,Role role){}
  @PostMapping("/register") public UserView register(@Valid @RequestBody RegisterRequest req){
    if(users.findByEmailIgnoreCase(req.email()).isPresent()) throw new ApiException(HttpStatus.CONFLICT,"This email is already registered");
    var u=new User(); u.setEmail(req.email().toLowerCase()); u.setPasswordHash(encoder.encode(req.password())); u.setDisplayName(req.name()); u.setRole(Role.STUDENT); u.setStatus(UserStatus.UNVERIFIED); users.save(u); tokens.sendVerification(u); return view(u);
  }
  @PostMapping("/login") public UserView login(@Valid @RequestBody LoginRequest req,HttpServletRequest request,HttpServletResponse response){
    loginLimits.check(req.email());
    var u=users.findByEmailIgnoreCase(req.email()).orElse(null); if(u==null){loginLimits.record(req.email(),request.getRemoteAddr(),false);throw new ApiException(HttpStatus.UNAUTHORIZED,"Incorrect email or password");}
    if(u.getStatus()!=UserStatus.ACTIVE||!encoder.matches(req.password(),u.getPasswordHash())){loginLimits.record(req.email(),request.getRemoteAddr(),false);throw new ApiException(HttpStatus.UNAUTHORIZED,"Incorrect email or password");}
    loginLimits.record(req.email(),request.getRemoteAddr(),true); issueSession(u,refreshTokens.issue(u),response); return view(u);
  }
  @PostMapping("/refresh") public UserView refresh(@CookieValue(name="treehole_refresh",required=false) String raw,HttpServletResponse response){ if(raw==null) throw new ApiException(HttpStatus.UNAUTHORIZED,"Your session has expired. Please sign in again"); var rotated=refreshTokens.rotate(raw); if(rotated.user().getStatus()!=UserStatus.ACTIVE) throw new ApiException(HttpStatus.FORBIDDEN,"This account is not active"); issueSession(rotated.user(),rotated.token(),response); return view(rotated.user()); }
  @PostMapping("/logout") public void logout(@CookieValue(name="treehole_refresh",required=false) String raw,HttpServletResponse response){ refreshTokens.revoke(raw); clearCookie("treehole_token",response); clearCookie("treehole_refresh",response); }
  @PostMapping("/verify") @Transactional public void verify(@RequestParam String token){ UUID userId=tokens.consume(token,"VERIFY_EMAIL"); var u=users.findById(userId).orElseThrow(); u.setStatus(UserStatus.ACTIVE); }
  @PostMapping("/forgot-password") public void forgot(@Valid @RequestBody EmailRequest req){ users.findByEmailIgnoreCase(req.email()).ifPresent(tokens::sendReset); }
  @PostMapping("/reset-password") public void reset(@Valid @RequestBody ResetRequest req){ UUID userId=tokens.consume(req.token(),"RESET_PASSWORD"); tokens.resetPassword(userId,req.password()); }
  @GetMapping("/me") public UserView me(){ return view(current.get()); }
  private UserView view(User u){ return new UserView(u.getId(),u.getEmail(),u.getDisplayName(),u.getRole()); }
  private void issueSession(User u,RefreshTokenService.Issued refresh,HttpServletResponse response){ response.addHeader(HttpHeaders.SET_COOKIE,ResponseCookie.from("treehole_token",jwt.create(u.getEmail(),u.getRole().name())).httpOnly(true).sameSite("Lax").path("/").maxAge(Duration.ofMinutes(15)).build().toString()); response.addHeader(HttpHeaders.SET_COOKIE,ResponseCookie.from("treehole_refresh",refresh.raw()).httpOnly(true).sameSite("Lax").path("/").maxAge(Duration.ofDays(30)).build().toString()); }
  private void clearCookie(String name,HttpServletResponse response){response.addHeader(HttpHeaders.SET_COOKIE,ResponseCookie.from(name,"").httpOnly(true).sameSite("Lax").path("/").maxAge(0).build().toString());}
}
