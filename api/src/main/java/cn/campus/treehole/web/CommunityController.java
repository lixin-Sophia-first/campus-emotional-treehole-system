package cn.campus.treehole.web;

import cn.campus.treehole.domain.Enums.*;
import cn.campus.treehole.repo.EmotionRecordRepository;
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
import java.util.*;

@RestController @RequestMapping("/api/v1") @RequiredArgsConstructor
public class CommunityController {
  private final JdbcTemplate jdbc; private final EmotionRecordRepository records; private final CurrentUserService current;
  public record TextRequest(@NotBlank @Size(max=1000) String content){}
  public record ReportRequest(@NotBlank String reason,@Size(max=1000) String details){}
  @GetMapping("/comments") public List<Map<String,Object>> comments(@RequestParam UUID recordId){ requirePublic(recordId); return jdbc.queryForList("select id,content,anonymous_alias as alias,created_at from comments where record_id=? and deleted_at is null order by created_at",recordId); }
  @PostMapping("/comments") @PreAuthorize("hasRole('STUDENT')") @Transactional public Map<String,Object> comment(@RequestParam UUID recordId,@Valid @RequestBody TextRequest req){ var r=requirePublic(recordId); var u=current.get(); String suffix=Integer.toHexString(Math.abs(Objects.hash(recordId,u.getId()))); if(suffix.length()>3)suffix=suffix.substring(0,3); String alias=r.getAuthor().getId().equals(u.getId())?"Author":"Harbor Friend "+suffix; var id=UUID.randomUUID(); jdbc.update("insert into comments(id,record_id,author_id,content,anonymous_alias) values (?,?,?,?,?)",id,recordId,u.getId(),req.content(),alias); return Map.of("id",id,"content",req.content(),"alias",alias); }
  @PostMapping("/reactions/{type}") @PreAuthorize("hasRole('STUDENT')") @Transactional public Map<String,Boolean> reaction(@PathVariable String type,@RequestParam UUID recordId){ requirePublic(recordId); var u=current.get(); String table=switch(type){case "like"->"likes";case "bookmark"->"bookmarks";default->throw new ApiException(HttpStatus.BAD_REQUEST,"Unsupported interaction type");}; int deleted=jdbc.update("delete from "+table+" where record_id=? and user_id=?",recordId,u.getId()); if(deleted==0) jdbc.update("insert into "+table+"(record_id,user_id) values (?,?)",recordId,u.getId()); return Map.of("active",deleted==0); }
  @PostMapping("/reports") @PreAuthorize("hasRole('STUDENT')") public void report(@RequestParam UUID recordId,@Valid @RequestBody ReportRequest req){ requirePublic(recordId); jdbc.update("insert into reports(record_id,reporter_id,reason,details) values (?,?,?,?)",recordId,current.get().getId(),req.reason(),req.details()==null?"":req.details()); }
  private cn.campus.treehole.domain.EmotionRecord requirePublic(UUID id){ var r=records.findByIdAndDeletedAtIsNull(id).orElseThrow(()->new ApiException(HttpStatus.NOT_FOUND,"Entry not found")); if(r.getVisibility()!=Visibility.PUBLIC||r.getModerationStatus()!=ModerationStatus.PUBLISHED) throw new ApiException(HttpStatus.NOT_FOUND,"Entry not found"); return r; }
}
