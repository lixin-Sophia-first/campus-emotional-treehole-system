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
  public record CaseView(UUID id,RiskLevel riskLevel,Visibility sourceVisibility,String publicContent,CaseStatus status,UUID assigneeId,String assigneeName,boolean mine,boolean identityAvailable,boolean studentDeclined,boolean falsePositive,String closureNote,Instant createdAt){}
  public record MessageRequest(@NotBlank @Size(max=2000) String content){}
  public record MessageView(UUID id,String senderType,String senderName,String content,Instant createdAt){}
  public record AccessRequest(@NotBlank @Size(max=500) String reason){}
  public record IdentityView(UUID studentId,String email,String name){}
  public record CloseRequest(@NotBlank @Size(max=2000) String note){}

  @GetMapping @Transactional(readOnly=true) public List<CaseView> list(){ var u=current.get();
    var source=u.getRole()==Role.STUDENT?cases.findByStudentIdOrderByCreatedAtDesc(u.getId()):cases.findAllByOrderByCreatedAtDesc();
    return source.stream().map(c->view(c,u)).toList();
  }
  @PostMapping("/{id}/claim") @PreAuthorize("hasRole('TEACHER')") @Transactional public CaseView claim(@PathVariable UUID id){
    var teacher=current.get(); var c=cases.findLockedById(id).orElseThrow(()->new ApiException(HttpStatus.NOT_FOUND,"Risk case not found"));
    if(c.getAssignee()!=null&&!c.getAssignee().getId().equals(teacher.getId())) throw new ApiException(HttpStatus.CONFLICT,"This case has already been claimed by another teacher");
    var from=c.getStatus();c.setAssignee(teacher); c.setStatus(CaseStatus.IN_PROGRESS); history(c,teacher,"CLAIM_CASE",from,c.getStatus(),"");audit(teacher,"CLAIM_CASE","RISK_CASE",c.getId(),"");notify(c.getStudent().getId(),"A verified teacher joined your care case","You can now use the anonymous care conversation.","/care?case="+id);return view(c,teacher);
  }
  @PostMapping("/{id}/assign/{teacherId}") @PreAuthorize("hasRole('ADMIN')") @Transactional public CaseView assign(@PathVariable UUID id,@PathVariable UUID teacherId){
    var admin=current.get(); var c=cases.findLockedById(id).orElseThrow(()->new ApiException(HttpStatus.NOT_FOUND,"Risk case not found"));
    var t=users.findById(teacherId).filter(x->x.getRole()==Role.TEACHER&&x.getStatus()==UserStatus.ACTIVE).orElseThrow(()->new ApiException(HttpStatus.BAD_REQUEST,"Active teacher not found"));var previous=c.getAssignee();var from=c.getStatus();c.setAssignee(t);c.setStatus(CaseStatus.IN_PROGRESS);history(c,admin,previous==null?"ASSIGN_CASE":"TRANSFER_CASE",from,c.getStatus(),"teacher="+teacherId);audit(admin,previous==null?"ASSIGN_CASE":"TRANSFER_CASE","RISK_CASE",id,"teacher="+teacherId);notify(t.getId(),"Risk case assigned","An administrator assigned a risk case to you.","/teacher?case="+id);notify(c.getStudent().getId(),"A verified teacher joined your care case","You can now use the anonymous care conversation.","/care?case="+id);return view(c,admin);
  }
  @GetMapping("/{id}/messages") @Transactional(readOnly=true) public List<MessageView> messageList(@PathVariable UUID id){ var u=current.get(); if(u.getRole()==Role.ADMIN)throw new ApiException(HttpStatus.FORBIDDEN,"Administrators cannot read care conversations");var c=authorizedCase(id,u); return messages.findByRiskCaseIdOrderByCreatedAtAsc(c.getId()).stream().map(m->new MessageView(m.getId(),m.getSender().getRole().name(),m.getSender().getRole()==Role.TEACHER?"Verified teacher":"Anonymous student",m.getContent(),m.getCreatedAt())).toList(); }
  @PostMapping("/{id}/messages") @Transactional public MessageView message(@PathVariable UUID id,@Valid @RequestBody MessageRequest req){var u=current.get();var c=authorizedCase(id,u);if(u.getRole()==Role.ADMIN)throw new ApiException(HttpStatus.FORBIDDEN,"Administrators cannot join care conversations");if(c.getStatus()==CaseStatus.CLOSED)throw new ApiException(HttpStatus.CONFLICT,"This case is closed");if(c.isStudentDeclined())throw new ApiException(HttpStatus.CONFLICT,"The student declined further conversation");var m=new CaseMessage();m.setRiskCase(c);m.setSender(u);m.setContent(req.content());messages.save(m);audit(u,"SEND_CARE_MESSAGE","RISK_CASE",id,"");if(u.getRole()==Role.TEACHER)notify(c.getStudent().getId(),"New care message","A verified teacher sent you a message.","/care?case="+id);else if(c.getAssignee()!=null)notify(c.getAssignee().getId(),"New anonymous student message","A student replied in a care conversation.","/teacher?case="+id);return new MessageView(m.getId(),u.getRole().name(),u.getRole()==Role.TEACHER?"Verified teacher":"Anonymous student",m.getContent(),m.getCreatedAt());}
  @PostMapping("/{id}/identity-request") @PreAuthorize("hasRole('TEACHER')") @Transactional public void requestIdentity(@PathVariable UUID id,@Valid @RequestBody AccessRequest req){
    var t=current.get(); var c=authorizedCase(id,t); if(c.getRiskLevel()!=RiskLevel.HIGH) throw new ApiException(HttpStatus.CONFLICT,"Identity requests are only required for HIGH-risk cases");if(access.existsByRiskCaseIdAndTeacherIdAndStatus(id,t.getId(),AccessStatus.PENDING)||access.existsByRiskCaseIdAndTeacherIdAndStatus(id,t.getId(),AccessStatus.APPROVED))throw new ApiException(HttpStatus.CONFLICT,"An identity request already exists");
    var a=new IdentityAccessRequest(); a.setRiskCase(c); a.setTeacher(t); a.setReason(req.reason()); access.save(a); audit(t,"REQUEST_IDENTITY","RISK_CASE",id,req.reason());
  }
  @GetMapping("/{id}/student-identity") @Transactional public IdentityView identity(@PathVariable UUID id){
    var u=current.get(); var c=cases.findById(id).orElseThrow(()->new ApiException(HttpStatus.NOT_FOUND,"Risk case not found"));
    boolean allowed=u.getRole()==Role.ADMIN || (u.getRole()==Role.TEACHER && c.getAssignee()!=null && c.getAssignee().getId().equals(u.getId()) && (c.getRiskLevel()==RiskLevel.CRITICAL || access.existsByRiskCaseIdAndTeacherIdAndStatus(id,u.getId(),AccessStatus.APPROVED)));
    if(!allowed) throw new ApiException(HttpStatus.FORBIDDEN,"You do not have permission to view this identity"); audit(u,"VIEW_STUDENT_IDENTITY","RISK_CASE",id,""); return new IdentityView(c.getStudent().getId(),c.getStudent().getEmail(),c.getStudent().getDisplayName());
  }
  @PostMapping("/{id}/followups") @PreAuthorize("hasRole('TEACHER')") @Transactional public void followup(@PathVariable UUID id,@Valid @RequestBody CloseRequest req){var t=current.get();var c=authorizedCase(id,t);jdbc.update("insert into case_followups(case_id,teacher_id,note) values (?,?,?)",id,t.getId(),req.note());history(c,t,"ADD_FOLLOWUP",c.getStatus(),c.getStatus(),"");}
  @GetMapping("/{id}/followups") @PreAuthorize("hasAnyRole('TEACHER','ADMIN')") public List<Map<String,Object>> followups(@PathVariable UUID id){authorizedCase(id,current.get());return jdbc.queryForList("select f.id,f.note,f.created_at,u.display_name teacher from case_followups f join users u on u.id=f.teacher_id where f.case_id=? order by f.created_at",id);}
  @GetMapping("/{id}/history") @PreAuthorize("hasAnyRole('TEACHER','ADMIN')") public List<Map<String,Object>> history(@PathVariable UUID id){authorizedCase(id,current.get());return jdbc.queryForList("select h.id,h.action,h.from_status,h.to_status,h.details,h.created_at,coalesce(u.display_name,'System') actor from risk_case_history h left join users u on u.id=h.actor_id where h.case_id=? order by h.created_at",id);}
  @PostMapping("/{id}/decline") @PreAuthorize("hasRole('STUDENT')") @Transactional public CaseView decline(@PathVariable UUID id){var s=current.get();var c=authorizedCase(id,s);c.setStudentDeclined(true);history(c,s,"STUDENT_DECLINED",c.getStatus(),c.getStatus(),"");audit(s,"STUDENT_DECLINED","RISK_CASE",id,"");return view(c,s);}
  @PostMapping("/{id}/propose-closure") @PreAuthorize("hasRole('TEACHER')") @Transactional public CaseView propose(@PathVariable UUID id,@Valid @RequestBody CloseRequest req){var t=current.get();var c=authorizedCase(id,t);var from=c.getStatus();c.setClosureNote(req.note());c.setStatus(CaseStatus.CLOSURE_PROPOSED);history(c,t,"PROPOSE_CLOSURE",from,c.getStatus(),req.note());audit(t,"PROPOSE_CLOSURE","RISK_CASE",id,"");notifyAdmins("Closure review requested","A teacher submitted a risk case closure recommendation.");return view(c,t);}
  @PostMapping("/{id}/return") @PreAuthorize("hasRole('ADMIN')") @Transactional public CaseView returnCase(@PathVariable UUID id,@Valid @RequestBody CloseRequest req){var a=current.get();var c=cases.findLockedById(id).orElseThrow(()->new ApiException(HttpStatus.NOT_FOUND,"Case not found"));var from=c.getStatus();c.setStatus(CaseStatus.IN_PROGRESS);history(c,a,"RETURN_CLOSURE",from,c.getStatus(),req.note());audit(a,"RETURN_CLOSURE","RISK_CASE",id,req.note());if(c.getAssignee()!=null)notify(c.getAssignee().getId(),"Closure proposal returned",req.note());return view(c,a);}
  @PostMapping("/{id}/close") @PreAuthorize("hasRole('ADMIN')") @Transactional public CaseView close(@PathVariable UUID id){var a=current.get();var c=cases.findLockedById(id).orElseThrow(()->new ApiException(HttpStatus.NOT_FOUND,"Case not found"));var from=c.getStatus();c.setStatus(CaseStatus.CLOSED);history(c,a,"CLOSE_CASE",from,c.getStatus(),"");audit(a,"CLOSE_CASE","RISK_CASE",id,"");notify(c.getStudent().getId(),"Care case closed","An administrator reviewed and closed the care case.");return view(c,a);}

  private RiskCase authorizedCase(UUID id,User u){ var c=cases.findById(id).orElseThrow(()->new ApiException(HttpStatus.NOT_FOUND,"Case not found")); boolean ok=u.getRole()==Role.ADMIN||(u.getRole()==Role.STUDENT&&c.getStudent().getId().equals(u.getId()))||(u.getRole()==Role.TEACHER&&c.getAssignee()!=null&&c.getAssignee().getId().equals(u.getId())); if(!ok) throw new ApiException(HttpStatus.FORBIDDEN,"You do not have access to this case"); return c; }
  private CaseView view(RiskCase c,User u){boolean identity=u.getRole()==Role.ADMIN||(u.getRole()==Role.TEACHER&&c.getAssignee()!=null&&c.getAssignee().getId().equals(u.getId())&&(c.getRiskLevel()==RiskLevel.CRITICAL||access.existsByRiskCaseIdAndTeacherIdAndStatus(c.getId(),u.getId(),AccessStatus.APPROVED)));RiskLevel visibleRisk=u.getRole()==Role.STUDENT?null:c.getRiskLevel();return new CaseView(c.getId(),visibleRisk,c.getSourceVisibility(),c.getPublicContentSnapshot(),c.getStatus(),c.getAssignee()==null?null:c.getAssignee().getId(),c.getAssignee()==null?null:c.getAssignee().getDisplayName(),c.getStudent().getId().equals(u.getId()),identity,c.isStudentDeclined(),c.isFalsePositive(),c.getClosureNote(),c.getCreatedAt());}
  private void audit(User actor,String action,String type,UUID target,String details){ jdbc.update("insert into audit_logs(actor_id,action,target_type,target_id,details) values (?,?,?,?,?)",actor.getId(),action,type,target,details); }
  private void history(RiskCase c,User actor,String action,CaseStatus from,CaseStatus to,String details){jdbc.update("insert into risk_case_history(case_id,actor_id,action,from_status,to_status,details) values (?,?,?,?,?,?)",c.getId(),actor.getId(),action,from==null?null:from.name(),to==null?null:to.name(),details);}
  private void notify(UUID user,String title,String content){notify(user,title,content,null);}
  private void notify(UUID user,String title,String content,String path){jdbc.update("insert into notifications(user_id,title,content,target_path) values (?,?,?,?)",user,title,content,path);}
  private void notifyAdmins(String title,String content){jdbc.update("insert into notifications(user_id,title,content,target_path) select id,?,?,? from users where role='ADMIN' and status='ACTIVE'",title,content,"/admin/risk-cases");}
}
