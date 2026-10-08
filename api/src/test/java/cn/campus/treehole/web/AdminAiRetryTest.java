package cn.campus.treehole.web;

import cn.campus.treehole.repo.IdentityAccessRequestRepository;
import cn.campus.treehole.repo.UserRepository;
import cn.campus.treehole.security.RefreshTokenService;
import cn.campus.treehole.service.AiModelClient;
import cn.campus.treehole.service.AiEnhancementService;
import cn.campus.treehole.service.CurrentUserService;
import cn.campus.treehole.web.ApiErrorHandler.ApiException;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AdminAiRetryTest {
  @Test void administratorCannotRetryPrivateEntryThroughExternalModel(){
    var jdbc=mock(JdbcTemplate.class);
    var model=mock(AiModelClient.class);
    UUID task=UUID.randomUUID();
    when(jdbc.queryForList(anyString(),eq(task))).thenReturn(List.of(Map.of("visibility","PRIVATE")));
    var controller=new AdminController(mock(IdentityAccessRequestRepository.class),mock(UserRepository.class),
      mock(CurrentUserService.class),mock(RefreshTokenService.class),model,mock(AiEnhancementService.class),jdbc);

    assertThatThrownBy(()->controller.retry(task)).isInstanceOf(ApiException.class)
      .hasMessageContaining("local analysis only");
    verify(jdbc,never()).update(anyString(),any(Object[].class));
  }

  @Test void legacyPrivateRuleReasonsDoNotRevealMatchedText(){
    var jdbc=mock(JdbcTemplate.class);UUID task=UUID.randomUUID();
    var row=new HashMap<String,Object>(Map.of("visibility","PRIVATE","reason","Self-harm expression: kill myself; Potential means: pills"));
    when(jdbc.queryForList(anyString(),eq(task))).thenReturn(List.of(row));
    var controller=new AdminController(mock(IdentityAccessRequestRepository.class),mock(UserRepository.class),
      mock(CurrentUserService.class),mock(RefreshTokenService.class),mock(AiModelClient.class),mock(AiEnhancementService.class),jdbc);

    var result=controller.aiResults(task).getFirst();
    assertThat(result.get("reason")).isEqualTo("Self-harm expression; Potential means");
    assertThat(result).doesNotContainKey("visibility");
  }
}
