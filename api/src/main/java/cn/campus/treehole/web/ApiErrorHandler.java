package cn.campus.treehole.web;

import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;
import java.util.*;
import org.slf4j.*;

@RestControllerAdvice
public class ApiErrorHandler {
  private static final Logger log=LoggerFactory.getLogger(ApiErrorHandler.class);
  public static class ApiException extends RuntimeException {
    public final HttpStatus status;
    public ApiException(HttpStatus status,String message){ super(message); this.status=status; }
  }
  @ExceptionHandler(ApiException.class) ResponseEntity<?> api(ApiException e){ return ResponseEntity.status(e.status).body(Map.of("message",e.getMessage(),"timestamp",Instant.now())); }
  @ExceptionHandler(MethodArgumentNotValidException.class) ResponseEntity<?> validation(MethodArgumentNotValidException e){
    var errors=e.getBindingResult().getFieldErrors().stream().collect(java.util.stream.Collectors.toMap(x->x.getField(),x->Optional.ofNullable(x.getDefaultMessage()).orElse("Invalid"),(a,b)->a));
    return ResponseEntity.badRequest().body(Map.of("message","Please check your input","errors",errors));
  }
  @ExceptionHandler(Exception.class) ResponseEntity<?> unexpected(Exception e){log.error("Unhandled API error",e);return ResponseEntity.internalServerError().body(Map.of("message","The server could not complete this request. Please try again; if it happens again, contact the administrator.","timestamp",Instant.now()));}
}
