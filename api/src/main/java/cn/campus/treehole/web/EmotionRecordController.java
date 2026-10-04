package cn.campus.treehole.web;

import cn.campus.treehole.domain.*;
import cn.campus.treehole.service.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;
import java.util.*;
import static cn.campus.treehole.domain.Enums.*;

@RestController @RequestMapping("/api/v1") @RequiredArgsConstructor
public class EmotionRecordController {
  private final EmotionRecordService service; private final CurrentUserService current; private final JdbcTemplate jdbc;
  public record CreateRequest(@NotBlank @Size(min=2,max=3000) String content,@NotNull Visibility visibility,@NotBlank String emotion,@Min(1) @Max(10) int intensity,String tags,Boolean anonymous){}
  public record View(UUID id,String content,Visibility visibility,String emotion,int intensity,List<String> tags,String alias,boolean anonymous,RiskLevel riskLevel,String supportCard,boolean recommendationEligible,Instant createdAt,boolean mine,long likeCount,long commentCount,boolean liked,boolean bookmarked){}

  @PostMapping("/emotion-records") @PreAuthorize("hasRole('STUDENT')") public View create(@Valid @RequestBody CreateRequest req){var u=current.get();return view(service.create(u,req.content(),req.visibility(),req.emotion(),req.intensity(),req.tags(),!Boolean.FALSE.equals(req.anonymous())),u);}
  @PutMapping("/emotion-records/{id}") @PreAuthorize("hasRole('STUDENT')") public View update(@PathVariable UUID id,@Valid @RequestBody CreateRequest req){var u=current.get();return view(service.update(u,id,req.content(),req.visibility(),req.emotion(),req.intensity(),req.tags(),!Boolean.FALSE.equals(req.anonymous())),u);}
  @GetMapping("/emotion-records/mine") @PreAuthorize("hasRole('STUDENT')") public Page<View> mine(@RequestParam(defaultValue="0") int page){var u=current.get();return service.mine(u.getId(),PageRequest.of(page,20)).map(x->view(x,u));}
  @GetMapping("/emotion-records/{id}") public View one(@PathVariable UUID id){var u=current.optional().orElse(null);return view(service.visible(u,id),u);}
  @DeleteMapping("/emotion-records/{id}") @PreAuthorize("hasRole('STUDENT')") public void delete(@PathVariable UUID id){service.delete(current.get(),id);}
  @GetMapping("/feed") public Page<View> feed(@RequestParam(defaultValue="recommended") String mode,@RequestParam(defaultValue="") String query,@RequestParam(defaultValue="") String tag,@RequestParam(defaultValue="0") int page){var u=current.optional().orElse(null);return service.feed(mode,query,tag,PageRequest.of(page,20)).map(x->view(x,u));}
  @GetMapping("/emotion-trends") @PreAuthorize("hasRole('STUDENT')") public List<Map<String,Object>> trends(@RequestParam(defaultValue="week") String period){var u=current.get();String interval="month".equals(period)?"30 days":"7 days";return jdbc.queryForList("select to_char(date_trunc('day',created_at),'YYYY-MM-DD') as label,round(avg(intensity),2) as average,count(*) as count from emotion_records where author_id=? and deleted_at is null and created_at>=now()-cast(? as interval) group by date_trunc('day',created_at) order by date_trunc('day',created_at)",u.getId(),interval);}
  @GetMapping("/bookmarks") @PreAuthorize("hasRole('STUDENT')") public List<View> bookmarks(){var u=current.get();var ids=jdbc.queryForList("select record_id from bookmarks where user_id=? order by created_at desc",UUID.class,u.getId());return ids.stream().map(id->service.visible(u,id)).map(r->view(r,u)).toList();}

  private View view(EmotionRecord r,User viewer){UUID viewerId=viewer==null?null:viewer.getId();Map<String,Object> counts=jdbc.queryForMap("select (select count(*) from likes where record_id=?) likes,(select count(*) from comments where record_id=? and deleted_at is null) comments",r.getId(),r.getId());boolean liked=viewerId!=null&&Boolean.TRUE.equals(jdbc.queryForObject("select exists(select 1 from likes where record_id=? and user_id=?)",Boolean.class,r.getId(),viewerId));boolean bookmarked=viewerId!=null&&Boolean.TRUE.equals(jdbc.queryForObject("select exists(select 1 from bookmarks where record_id=? and user_id=?)",Boolean.class,r.getId(),viewerId));RiskLevel visibleRisk=viewer!=null&&viewer.getRole()!=Role.STUDENT?r.getRiskLevel():null;return new View(r.getId(),r.getContent(),r.getVisibility(),r.getEmotion(),r.getIntensity(),r.getTags().isBlank()?List.of():Arrays.stream(r.getTags().split(",")).map(String::trim).filter(s->!s.isBlank()).toList(),r.getAnonymousAlias(),r.isAnonymous(),visibleRisk,r.getSupportCard(),r.isRecommendationEligible(),r.getCreatedAt(),viewerId!=null&&r.getAuthor().getId().equals(viewerId),((Number)counts.get("likes")).longValue(),((Number)counts.get("comments")).longValue(),liked,bookmarked);}
}
