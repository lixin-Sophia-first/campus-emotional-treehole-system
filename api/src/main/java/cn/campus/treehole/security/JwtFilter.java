package cn.campus.treehole.security;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.*;

@Component @RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {
  private final JwtService jwt;
  @Override protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain) throws ServletException, IOException {
    String token=null;
    String auth=req.getHeader("Authorization");
    if(auth!=null && auth.startsWith("Bearer ")) token=auth.substring(7);
    if(token==null && req.getCookies()!=null) for(Cookie c:req.getCookies()) if("treehole_token".equals(c.getName())) token=c.getValue();
    if(token!=null) try {
      var claims=jwt.parse(token);
      var principal=claims.getSubject();
      var role=String.valueOf(claims.get("role"));
      SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(principal,null,List.of(new SimpleGrantedAuthority("ROLE_"+role))));
    } catch(Exception ignored) {}
    chain.doFilter(req,res);
  }
}

