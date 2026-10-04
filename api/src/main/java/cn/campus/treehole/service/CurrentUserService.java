package cn.campus.treehole.service;
import cn.campus.treehole.domain.User;
import cn.campus.treehole.repo.UserRepository;
import cn.campus.treehole.web.ApiErrorHandler.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import java.util.Optional;
@Service @RequiredArgsConstructor
public class CurrentUserService {
  private final UserRepository users;
  public User get(){
    var a=SecurityContextHolder.getContext().getAuthentication();
    if(a==null||!a.isAuthenticated()) throw new ApiException(HttpStatus.UNAUTHORIZED,"Please sign in first");
    var user=users.findByEmailIgnoreCase(a.getName()).orElseThrow(()->new ApiException(HttpStatus.UNAUTHORIZED,"Account not found"));
    if(user.getStatus()!=cn.campus.treehole.domain.Enums.UserStatus.ACTIVE) throw new ApiException(HttpStatus.FORBIDDEN,"This account is not active");
    return user;
  }
  public Optional<User> optional(){try{return Optional.of(get());}catch(ApiException e){return Optional.empty();}}
}
