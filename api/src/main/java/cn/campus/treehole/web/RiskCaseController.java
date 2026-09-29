package cn.campus.treehole.web;

import cn.campus.treehole.domain.*;
import cn.campus.treehole.repo.*;
import cn.campus.treehole.service.CurrentUserService;
import cn.campus.treehole.web.ApiErrorHandler.ApiException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.time.*;
import java.util.*;
import static cn.campus.treehole.domain.Enums.*;

@RestController @RequestMapping("/api/v1/risk-cases") @RequiredArgsConstructor
public class RiskCaseController {
  private final RiskCaseRepository cases; private final UserRepository users; private final CaseMessageRepository messages;
  private final IdentityAccessRequestRepository access; private final CurrentUserService current; private final JdbcTemplate jdbc;
  public record CaseView(UUID id,RiskLevel riskLevel,Visibility sourceVisibility,String publicContent,CaseStatus status,UUID assigneeId,String assigneeName,boolean mine,boolean identityAvailable,Instant createdAt){}
  public record MessageRequest(@NotBlank @Size(max=2000) String content){}
  public record MessageView(UUID id,String senderType,String senderName,String content,Instant createdAt){}
  public record AccessRequest(@NotBlank @Size(max=500) String reason){}
  public record IdentityView(UUID studentId,String email,String name){}
  public record CloseRequest(@NotBlank @Size(max=2000) String note){}

  @GetMapping public List<CaseView> list(){ var u=current.get();
    var source=u.getRole()==Role.STUDENT?cases.findByStudentIdOrderByCreatedAtDesc(u.getId()):cases.findAllByOrderByCreatedAtDesc();
    return source.stream().map(c->view(c,u)).toList();
  }
  @PostMapping("/{id}/claim") @PreAuthorize("hasRole('TEACHER')") @Transactional public CaseView claim(@PathVariable UUID id){
    var teacher=current.get(); var c=cases.findLockedById(id).orElseThrow(()->new ApiException(HttpStatus.NOT_FOUND,"Risk case not found"));
    if(c.getAssignee()!=null&&!c.getAssignee().getId().equals(teacher.getId())) throw new ApiException(HttpStatus.CONFLICT,"This case has already been claimed by another teacher");
    c.setAssignee(teacher); c.setStatus(CaseStatus.IN_PROGRESS); audit(teacher,"CLAIM_CASE","RISK_CASE",c.getId(),""); return view(c,teacher);
  }
  @PostMapping("/{id}/assign/{teacherId}") @PreAuthorize("hasRole('ADMIN')") @Transactional public CaseView assign(@PathVariable UUID id,@PathVariable UUID teacherId){
    var admin=current.get(); var c=cases.findLockedById(id).orElseThrow(()->new ApiException(HttpStatus.NOT_FOUND,"Risk case not found"));
    var t=users.findById(teacherId).filter(x->x.getRole()==Role.TEACHER).orElseThrow(()->new ApiException(HttpStatus.BAD_REQUEST,"Teacher not found")); c.setAssignee(t); c.setStatus(CaseStatus.IN_PROGRESS); audit(admin,"ASSIGN_CASE","RISK_CASE",id,"teacher="+teacherId); return view(c,admin);
  }
  @GetMapping("/{id}/messages") public List<MessageView> messageList(@PathVariable UUID id){ var u=current.get(); var c=authorizedCase(id,u); return messages.findByRiskCaseIdOrderByCreatedAtAsc(c.getId()).stream().map(m->new MessageView(m.getId(),m.getSender().getRole().name(),m.getSender().getRole()==Role.TEACHER?"Verified teacher":"Anonymous student",m.getContent(),m.getCreatedAt())).toList(); }
  @PostMapping("/{id}/messages") @Transactional public MessageView message(@PathVariable UUID id,@Valid @RequestBody MessageRequest req){ var u=current.get(); var c=authorizedCase(id,u); if(c.getStatus()==CaseStatus.CLOSED) throw new ApiException(HttpStatus.CONFLICT,"This case is closed"); var m=new CaseMessage(); m.setRiskCase(c); m.setSender(u); m.setContent(req.content()); messages.save(m); return new MessageView(m.getId(),u.getRole().name(),u.getRole()==Role.TEACHER?"Verified teacher":"Anonymous student",m.getContent(),m.getCreatedAt()); }
  @PostMapping("/{id}/identity-request") @PreAuthorize("hasRole('TEACHER')") @Transactional public void requestIdentity(@PathVariable UUID id,@Valid @RequestBody AccessRequest req){
    var t=current.get(); var c=authorizedCase(id,t); if(c.getRiskLevel()==RiskLevel.CRITICAL) throw new ApiException(HttpStatus.CONFLICT,"The assignee already has automatic identity access for a critical case");
    var a=new IdentityAccessRequest(); a.setRiskCase(c); a.setTeacher(t); a.setReason(req.reason()); access.save(a); audit(t,"REQUEST_IDENTITY","RISK_CASE",id,req.reason());
  }
  @GetMapping("/{id}/student-identity") public IdentityView identity(@PathVariable UUID id){
    var u=current.get(); var c=cases.findById(id).orElseThrow(()->new ApiException(HttpStatus.NOT_FOUND,"Risk case not found"));
    boolean allowed=u.getRole()==Role.ADMIN || (u.getRole()==Role.TEACHER && c.getAssignee()!=null && c.getAssignee().getId().equals(u.getId()) && (c.getRiskLevel()==RiskLevel.CRITICAL || access.existsByRiskCaseIdAndTeacherIdAndStatus(id,u.getId(),AccessStatus.APPROVED)));
    if(!allowed) throw new ApiException(HttpStatus.FORBIDDEN,"You do not have permission to view this identity"); audit(u,"VIEW_STUDENT_IDENTITY","RISK_CASE",id,""); return new IdentityView(c.getStudent().getId(),c.getStudent().getEmail(),c.getStudent().getDisplayName());
  }
  @PostMapping("/{id}/propose-closure") @PreAuthorize("hasRole('TEACHER')") @Transactional public CaseView propose(@PathVariable UUID id,@Valid @RequestBody CloseRequest req){ var t=current.get(); var c=authorizedCase(id,t); c.setClosureNote(req.note()); c.setStatus(CaseStatus.CLOSURE_PROPOSED); audit(t,"PROPOSE_CLOSURE","RISK_CASE",id,""); return view(c,t); }
  @PostMapping("/{id}/close") @PreAuthorize("hasRole('ADMIN')") @Transactional public CaseView close(@PathVariable UUID id){ var a=current.get(); var c=cases.findLockedById(id).orElseThrow(()->new ApiException(HttpStatus.NOT_FOUND,"Case not found")); c.setStatus(CaseStatus.CLOSED); audit(a,"CLOSE_CASE","RISK_CASE",id,""); return view(c,a); }

  private RiskCase authorizedCase(UUID id,User u){ var c=cases.findById(id).orElseThrow(()->new ApiException(HttpStatus.NOT_FOUND,"Case not found")); boolean ok=u.getRole()==Role.ADMIN||(u.getRole()==Role.STUDENT&&c.getStudent().getId().equals(u.getId()))||(u.getRole()==Role.TEACHER&&c.getAssignee()!=null&&c.getAssignee().getId().equals(u.getId())); if(!ok) throw new ApiException(HttpStatus.FORBIDDEN,"You do not have access to this case"); return c; }
  private CaseView view(RiskCase c,User u){ boolean identity=u.getRole()==Role.ADMIN||(u.getRole()==Role.TEACHER&&c.getAssignee()!=null&&c.getAssignee().getId().equals(u.getId())&&(c.getRiskLevel()==RiskLevel.CRITICAL||access.existsByRiskCaseIdAndTeacherIdAndStatus(c.getId(),u.getId(),AccessStatus.APPROVED))); return new CaseView(c.getId(),c.getRiskLevel(),c.getSourceVisibility(),c.getPublicContentSnapshot(),c.getStatus(),c.getAssignee()==null?null:c.getAssignee().getId(),c.getAssignee()==null?null:c.getAssignee().getDisplayName(),c.getStudent().getId().equals(u.getId()),identity,c.getCreatedAt()); }
  private void audit(User actor,String action,String type,UUID target,String details){ jdbc.update("insert into audit_logs(actor_id,action,target_type,target_id,details) values (?,?,?,?,?)",actor.getId(),action,type,target,details); }
}
