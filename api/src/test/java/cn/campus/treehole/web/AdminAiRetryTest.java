package cn.campus.treehole.web;

import cn.campus.treehole.repo.IdentityAccessRequestRepository;
import cn.campus.treehole.repo.UserRepository;
import cn.campus.treehole.security.RefreshTokenService;
import cn.campus.treehole.service.AiModelClient;
import cn.campus.treehole.service.CurrentUserService;
import cn.campus.treehole.web.ApiErrorHandler.ApiException;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AdminAiRetryTest {
  @Test void administratorCannotRetryPrivateEntryThroughExternalModel(){
    var jdbc=mock(JdbcTemplate.class);
    var model=mock(AiModelClient.class);
    UUID task=UUID.randomUUID();
    when(jdbc.queryForList(anyString(),eq(task))).thenReturn(List.of(Map.of("visibility","PRIVATE")));
    var controller=new AdminController(mock(IdentityAccessRequestRepository.class),mock(UserRepository.class),
      mock(CurrentUserService.class),mock(RefreshTokenService.class),model,jdbc);

    assertThatThrownBy(()->controller.retry(task)).isInstanceOf(ApiException.class)
      .hasMessageContaining("local analysis only");
    verify(jdbc,never()).update(anyString(),any(Object[].class));
  }
}
