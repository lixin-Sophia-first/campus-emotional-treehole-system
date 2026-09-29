package cn.campus.treehole.service;
import cn.campus.treehole.domain.User;
import cn.campus.treehole.repo.UserRepository;
import cn.campus.treehole.web.ApiErrorHandler.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
@Service @RequiredArgsConstructor
public class CurrentUserService {
  private final UserRepository users;
  public User get(){
    var a=SecurityContextHolder.getContext().getAuthentication();
    if(a==null||!a.isAuthenticated()) throw new ApiException(HttpStatus.UNAUTHORIZED,"请先登录");
    return users.findByEmailIgnoreCase(a.getName()).orElseThrow(()->new ApiException(HttpStatus.UNAUTHORIZED,"账号不存在"));
  }
}

