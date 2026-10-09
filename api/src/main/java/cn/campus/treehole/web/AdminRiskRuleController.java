package cn.campus.treehole.web;

import cn.campus.treehole.service.CurrentUserService;
import cn.campus.treehole.service.RiskRuleCandidateService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/risk-rule-candidates")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminRiskRuleController {
  private final RiskRuleCandidateService candidates;
  private final CurrentUserService current;

  @GetMapping public List<Map<String,Object>> list(){return candidates.list();}
  @PostMapping("/{id}/confirm") public void confirm(@PathVariable UUID id){candidates.confirm(id,current.get().getId());}
  @PostMapping("/{id}/reject") public void reject(@PathVariable UUID id){candidates.reject(id,current.get().getId());}
  @PostMapping("/{id}/draft") public void draft(@PathVariable UUID id){candidates.draft(id,current.get().getId());}
  @PostMapping("/{id}/activate") public void activate(@PathVariable UUID id){candidates.activate(id,current.get().getId());}
  @PostMapping("/{id}/disable") public void disable(@PathVariable UUID id){candidates.disable(id,current.get().getId());}
}
