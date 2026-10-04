package cn.campus.treehole.security;

import cn.campus.treehole.web.ApiErrorHandler.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service @RequiredArgsConstructor
public class LoginRateLimitService {
  private final JdbcTemplate jdbc;
  public void check(String email){ Integer failures=jdbc.queryForObject("select count(*) from login_attempts where lower(email)=lower(?) and successful=false and created_at>now()-interval '15 minutes'",Integer.class,email); if(failures!=null&&failures>=5) throw new ApiException(HttpStatus.TOO_MANY_REQUESTS,"Too many failed attempts. Try again in 15 minutes"); }
  public void record(String email,String ip,boolean success){ jdbc.update("insert into login_attempts(email,ip_address,successful) values (?,?,?)",email==null?"":email.toLowerCase(),ip==null?"":ip,success); if(success) jdbc.update("delete from login_attempts where lower(email)=lower(?) and successful=false",email); }
}
