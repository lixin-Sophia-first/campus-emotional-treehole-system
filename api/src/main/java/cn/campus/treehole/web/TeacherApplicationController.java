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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.util.*;
import java.time.Instant;
import static cn.campus.treehole.domain.Enums.*;

@RestController @RequestMapping("/api/v1/teacher-applications") @RequiredArgsConstructor
public class TeacherApplicationController {
  private final TeacherApplicationRepository applications; private final UserRepository users; private final PasswordEncoder encoder; private final CurrentUserService current; private final JdbcTemplate jdbc;
  public record ApplyRequest(@Email String email,@Size(min=8,max=72) String password,@NotBlank String name,@NotBlank String employeeNumber,@NotBlank String department,@NotBlank @Size(max=1000) String statement){}
  public record View(UUID id,String email,String name,String employeeNumber,String department,String statement,ApplicationStatus status){}
  @PostMapping public View apply(@Valid @RequestBody ApplyRequest req){
    if(users.findByEmailIgnoreCase(req.email()).isPresent()||applications.existsByEmailIgnoreCase(req.email())) throw new ApiException(HttpStatus.CONFLICT,"This email is already in use or under review");
    var a=new TeacherApplication(); a.setEmail(req.email().toLowerCase()); a.setPasswordHash(encoder.encode(req.password())); a.setName(req.name()); a.setEmployeeNumber(req.employeeNumber()); a.setDepartment(req.department()); a.setStatement(req.statement()); return view(applications.save(a));
  }
  @GetMapping @PreAuthorize("hasRole('ADMIN')") public List<View> list(){ return applications.findAllByOrderByCreatedAtDesc().stream().map(this::view).toList(); }
  @PostMapping("/{id}/approve") @PreAuthorize("hasRole('ADMIN')") @Transactional public View approve(@PathVariable UUID id){
    var a=applications.findById(id).orElseThrow(()->new ApiException(HttpStatus.NOT_FOUND,"Application not found")); if(a.getStatus()!=ApplicationStatus.PENDING) throw new ApiException(HttpStatus.CONFLICT,"This application has already been reviewed");
    var admin=current.get();var u=new User();u.setEmail(a.getEmail());u.setPasswordHash(a.getPasswordHash());u.setDisplayName(a.getName());u.setRole(Role.TEACHER);u.setDepartment(a.getDepartment());u.setEmployeeNumber(a.getEmployeeNumber());users.save(u);a.setStatus(ApplicationStatus.APPROVED);a.setReviewedBy(admin);a.setReviewedAt(Instant.now());jdbc.update("insert into notifications(user_id,title,content) values (?,?,?)",u.getId(),"Teacher application approved","Your verified teacher workspace is now available.");audit(admin,"APPROVE_TEACHER_APPLICATION",a.getId());return view(a);
  }
  @PostMapping("/{id}/reject") @PreAuthorize("hasRole('ADMIN')") @Transactional public View reject(@PathVariable UUID id){var a=applications.findById(id).orElseThrow(()->new ApiException(HttpStatus.NOT_FOUND,"Application not found"));var admin=current.get();a.setStatus(ApplicationStatus.REJECTED);a.setReviewedBy(admin);a.setReviewedAt(Instant.now());audit(admin,"REJECT_TEACHER_APPLICATION",a.getId());return view(a);}
  private void audit(User actor,String action,UUID id){jdbc.update("insert into audit_logs(actor_id,action,target_type,target_id,details) values (?,?,?,?,?)",actor.getId(),action,"TEACHER_APPLICATION",id,"");}
  private View view(TeacherApplication a){return new View(a.getId(),a.getEmail(),a.getName(),a.getEmployeeNumber(),a.getDepartment(),a.getStatement(),a.getStatus());}
}
