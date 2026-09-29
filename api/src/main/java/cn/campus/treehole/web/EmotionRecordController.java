package cn.campus.treehole.web;

import cn.campus.treehole.domain.EmotionRecord;
import cn.campus.treehole.service.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;
import java.util.*;
import static cn.campus.treehole.domain.Enums.*;

@RestController @RequestMapping("/api/v1") @RequiredArgsConstructor
public class EmotionRecordController {
  private final EmotionRecordService service; private final CurrentUserService current;
  public record CreateRequest(@NotBlank @Size(min=2,max=3000) String content,@NotNull Visibility visibility,@NotBlank String emotion,@Min(1) @Max(10) int intensity,String tags){}
  public record View(UUID id,String content,Visibility visibility,String emotion,int intensity,List<String> tags,String alias,RiskLevel riskLevel,String supportCard,boolean recommendationEligible,Instant createdAt,boolean mine){}
  @PostMapping("/emotion-records") @PreAuthorize("hasRole('STUDENT')") public View create(@Valid @RequestBody CreateRequest req){ var u=current.get(); return view(service.create(u,req.content(),req.visibility(),req.emotion(),req.intensity(),req.tags()),u.getId()); }
  @GetMapping("/emotion-records/mine") @PreAuthorize("hasRole('STUDENT')") public Page<View> mine(@RequestParam(defaultValue="0") int page){ var u=current.get(); return service.mine(u.getId(),PageRequest.of(page,20)).map(x->view(x,u.getId())); }
  @GetMapping("/emotion-records/{id}") public View one(@PathVariable UUID id){ var u=current.get(); return view(service.visible(u,id),u.getId()); }
  @DeleteMapping("/emotion-records/{id}") @PreAuthorize("hasRole('STUDENT')") public void delete(@PathVariable UUID id){ service.delete(current.get(),id); }
  @GetMapping("/feed") public Page<View> feed(@RequestParam(defaultValue="recommended") String mode,@RequestParam(defaultValue="0") int page){ var u=current.get(); return service.feed(!"latest".equals(mode),PageRequest.of(page,20)).map(x->view(x,u.getId())); }
  private View view(EmotionRecord r,UUID viewer){ return new View(r.getId(),r.getContent(),r.getVisibility(),r.getEmotion(),r.getIntensity(),r.getTags().isBlank()?List.of():Arrays.asList(r.getTags().split(",")),r.getAnonymousAlias(),r.getRiskLevel(),r.getSupportCard(),r.isRecommendationEligible(),r.getCreatedAt(),r.getAuthor().getId().equals(viewer)); }
}

