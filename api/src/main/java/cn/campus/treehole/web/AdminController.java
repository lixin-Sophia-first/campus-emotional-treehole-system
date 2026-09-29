package cn.campus.treehole.web;

import cn.campus.treehole.repo.*;
import cn.campus.treehole.domain.Enums.Role;
import cn.campus.treehole.service.CurrentUserService;
import cn.campus.treehole.web.ApiErrorHandler.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;
import java.util.*;
import static cn.campus.treehole.domain.Enums.*;

@RestController @RequestMapping("/api/v1/admin") @PreAuthorize("hasRole('ADMIN')") @RequiredArgsConstructor
public class AdminController {
  private final IdentityAccessRequestRepository access; private final UserRepository users; private final CurrentUserService current; private final JdbcTemplate jdbc;
  public record AccessView(UUID id,UUID caseId,String teacherName,String reason,AccessStatus status){}
  public record UserView(UUID id,String email,String name,Role role,String status){}
  @GetMapping("/users") public List<UserView> users(@RequestParam(required=false) Role role){ return users.findAll().stream().filter(u->role==null||u.getRole()==role).map(u->new UserView(u.getId(),u.getEmail(),u.getDisplayName(),u.getRole(),u.getStatus().name())).toList(); }
  @GetMapping("/identity-requests") public List<AccessView> requests(){ return access.findAllByOrderByCreatedAtDesc().stream().map(a->new AccessView(a.getId(),a.getRiskCase().getId(),a.getTeacher().getDisplayName(),a.getReason(),a.getStatus())).toList(); }
  @PostMapping("/identity-requests/{id}/{decision}") @Transactional public AccessView decide(@PathVariable UUID id,@PathVariable String decision){ var a=access.findById(id).orElseThrow(()->new ApiException(HttpStatus.NOT_FOUND,"申请不存在")); a.setStatus("approve".equals(decision)?AccessStatus.APPROVED:AccessStatus.REJECTED); a.setReviewedBy(current.get()); a.setReviewedAt(Instant.now()); jdbc.update("insert into audit_logs(actor_id,action,target_type,target_id,details) values (?,?,?,?,?)",current.get().getId(),"IDENTITY_"+a.getStatus(),"IDENTITY_REQUEST",a.getId(),""); return new AccessView(a.getId(),a.getRiskCase().getId(),a.getTeacher().getDisplayName(),a.getReason(),a.getStatus()); }
  @GetMapping("/audit-logs") public List<Map<String,Object>> logs(){ return jdbc.queryForList("select id,action,target_type,target_id,details,created_at from audit_logs order by created_at desc limit 200"); }
  @GetMapping("/reports") public List<Map<String,Object>> reports(){ return jdbc.queryForList("select r.id,r.record_id,r.reason,r.details,r.status,r.created_at,e.content from reports r join emotion_records e on e.id=r.record_id order by r.created_at desc"); }
  @PostMapping("/reports/{id}/{decision}") @Transactional public void reportDecision(@PathVariable UUID id,@PathVariable String decision){ var rows=jdbc.queryForList("select record_id from reports where id=?",id); if(rows.isEmpty()) throw new ApiException(HttpStatus.NOT_FOUND,"举报不存在"); UUID record=(UUID)rows.getFirst().get("record_id"); if("hide".equals(decision)) jdbc.update("update emotion_records set moderation_status='HIDDEN' where id=?",record); jdbc.update("update reports set status=? where id=?","hide".equals(decision)?"ACCEPTED":"REJECTED",id); jdbc.update("insert into audit_logs(actor_id,action,target_type,target_id,details) values (?,?,?,?,?)",current.get().getId(),"REPORT_"+decision.toUpperCase(),"REPORT",id,""); }
}
