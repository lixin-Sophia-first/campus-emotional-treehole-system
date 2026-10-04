package cn.campus.treehole.security;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.*;

@Component
public class CsrfCookieFilter extends OncePerRequestFilter {
  private static final String COOKIE="treehole_csrf",HEADER="X-CSRF-Token";
  private static final Set<String> EXEMPT=Set.of("/api/v1/auth/login","/api/v1/auth/register","/api/v1/auth/forgot-password","/api/v1/auth/verify","/api/v1/auth/reset-password");
  private final SecureRandom random=new SecureRandom();
  @Override protected void doFilterInternal(HttpServletRequest req,HttpServletResponse res,FilterChain chain)throws ServletException,IOException{
    String cookie=cookie(req,COOKIE); if(cookie==null){byte[] b=new byte[24];random.nextBytes(b);cookie=Base64.getUrlEncoder().withoutPadding().encodeToString(b);res.addHeader(HttpHeaders.SET_COOKIE,ResponseCookie.from(COOKIE,cookie).httpOnly(false).sameSite("Lax").path("/").maxAge(Duration.ofDays(1)).build().toString());}
    boolean unsafe=!Set.of("GET","HEAD","OPTIONS").contains(req.getMethod());
    if(unsafe&&!EXEMPT.contains(req.getRequestURI())&&!Objects.equals(cookie,req.getHeader(HEADER))){res.sendError(HttpServletResponse.SC_FORBIDDEN,"Invalid CSRF token");return;}
    chain.doFilter(req,res);
  }
  private String cookie(HttpServletRequest req,String name){if(req.getCookies()!=null)for(var c:req.getCookies())if(name.equals(c.getName()))return c.getValue();return null;}
}
