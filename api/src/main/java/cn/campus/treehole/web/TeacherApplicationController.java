package cn.campus.treehole.web;

import cn.campus.treehole.domain.*;
import cn.campus.treehole.repo.*;
import cn.campus.treehole.service.CurrentUserService;
import cn.campus.treehole.web.ApiErrorHandler.ApiException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.util.*;
import static cn.campus.treehole.domain.Enums.*;

@RestController @RequestMapping("/api/v1/teacher-applications") @RequiredArgsConstructor
public class TeacherApplicationController {
  private final TeacherApplicationRepository applications; private final UserRepository users; private final PasswordEncoder encoder; private final CurrentUserService current;
  public record ApplyRequest(@Email String email,@Size(min=8,max=72) String password,@NotBlank String name,@NotBlank String employeeNumber,@NotBlank String department,@NotBlank @Size(max=1000) String statement){}
  public record View(UUID id,String email,String name,String employeeNumber,String department,String statement,ApplicationStatus status){}
  @PostMapping public View apply(@Valid @RequestBody ApplyRequest req){
    if(users.findByEmailIgnoreCase(req.email()).isPresent()||applications.existsByEmailIgnoreCase(req.email())) throw new ApiException(HttpStatus.CONFLICT,"This email is already in use or under review");
    var a=new TeacherApplication(); a.setEmail(req.email().toLowerCase()); a.setPasswordHash(encoder.encode(req.password())); a.setName(req.name()); a.setEmployeeNumber(req.employeeNumber()); a.setDepartment(req.department()); a.setStatement(req.statement()); return view(applications.save(a));
  }
  @GetMapping @PreAuthorize("hasRole('ADMIN')") public List<View> list(){ return applications.findAllByOrderByCreatedAtDesc().stream().map(this::view).toList(); }
  @PostMapping("/{id}/approve") @PreAuthorize("hasRole('ADMIN')") @Transactional public View approve(@PathVariable UUID id){
    var a=applications.findById(id).orElseThrow(()->new ApiException(HttpStatus.NOT_FOUND,"Application not found")); if(a.getStatus()!=ApplicationStatus.PENDING) throw new ApiException(HttpStatus.CONFLICT,"This application has already been reviewed");
    var u=new User(); u.setEmail(a.getEmail()); u.setPasswordHash(a.getPasswordHash()); u.setDisplayName(a.getName()); u.setRole(Role.TEACHER); u.setDepartment(a.getDepartment()); u.setEmployeeNumber(a.getEmployeeNumber()); users.save(u); a.setStatus(ApplicationStatus.APPROVED); return view(a);
  }
  @PostMapping("/{id}/reject") @PreAuthorize("hasRole('ADMIN')") @Transactional public View reject(@PathVariable UUID id){ var a=applications.findById(id).orElseThrow(()->new ApiException(HttpStatus.NOT_FOUND,"Application not found")); a.setStatus(ApplicationStatus.REJECTED); return view(a); }
  private View view(TeacherApplication a){return new View(a.getId(),a.getEmail(),a.getName(),a.getEmployeeNumber(),a.getDepartment(),a.getStatement(),a.getStatus());}
}
