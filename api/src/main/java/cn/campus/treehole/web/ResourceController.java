package cn.campus.treehole.web;
import cn.campus.treehole.domain.SupportResource;
import cn.campus.treehole.repo.SupportResourceRepository;
import cn.campus.treehole.service.CurrentUserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;
import java.util.*;
@RestController @RequestMapping("/api/v1/resources") @RequiredArgsConstructor
public class ResourceController {
  private final SupportResourceRepository resources; private final CurrentUserService current;
  public record Request(@NotBlank String title,@NotBlank String summary,@NotBlank String content,@NotBlank String type){}
  public record View(UUID id,String title,String summary,String content,String type,String author,Instant updatedAt){}
  @GetMapping public List<View> list(){ return resources.findByActiveTrueOrderByUpdatedAtDesc().stream().map(this::view).toList(); }
  @PostMapping @PreAuthorize("hasAnyRole('TEACHER','ADMIN')") public View create(@Valid @RequestBody Request req){ var r=new SupportResource(); r.setAuthor(current.get()); r.setTitle(req.title()); r.setSummary(req.summary()); r.setContent(req.content()); r.setResourceType(req.type()); return view(resources.save(r)); }
  private View view(SupportResource r){ return new View(r.getId(),r.getTitle(),r.getSummary(),r.getContent(),r.getResourceType(),r.getAuthor().getDisplayName(),r.getUpdatedAt()); }
}

