package cn.campus.treehole.web;

import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;
import java.util.*;

@RestControllerAdvice
public class ApiErrorHandler {
  public static class ApiException extends RuntimeException {
    public final HttpStatus status;
    public ApiException(HttpStatus status,String message){ super(message); this.status=status; }
  }
  @ExceptionHandler(ApiException.class) ResponseEntity<?> api(ApiException e){ return ResponseEntity.status(e.status).body(Map.of("message",e.getMessage(),"timestamp",Instant.now())); }
  @ExceptionHandler(MethodArgumentNotValidException.class) ResponseEntity<?> validation(MethodArgumentNotValidException e){
    var errors=e.getBindingResult().getFieldErrors().stream().collect(java.util.stream.Collectors.toMap(x->x.getField(),x->Optional.ofNullable(x.getDefaultMessage()).orElse("无效"),(a,b)->a));
    return ResponseEntity.badRequest().body(Map.of("message","输入内容有误","errors",errors));
  }
}

