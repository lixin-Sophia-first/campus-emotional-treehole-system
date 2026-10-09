package cn.campus.treehole.service;

import cn.campus.treehole.web.ApiErrorHandler.ApiException;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class RiskRuleCandidateServiceTest {
  private final JdbcTemplate jdbc=mock(JdbcTemplate.class);
  private final AiModelClient model=mock(AiModelClient.class);
  private final RiskRuleCandidateService service=new RiskRuleCandidateService(jdbc,model,new RiskAnalysisService(List::of));

  @Test void neverSendsAWithdrawnOrPrivateSourceForDrafting(){
    UUID id=UUID.randomUUID();UUID record=UUID.randomUUID();
    when(jdbc.queryForList(anyString(),eq(id))).thenReturn(List.of(Map.of("record_id",record,"status","CONFIRMED")));
    when(jdbc.queryForList(anyString(),eq(record))).thenReturn(List.of());
    when(model.configured()).thenReturn(true);

    assertThatThrownBy(()->service.draft(id,UUID.randomUUID())).isInstanceOf(ApiException.class)
      .hasMessageContaining("no longer a public post");
    verify(model,never()).suggestRiskPhrase(anyString());
  }

  @Test void rejectsAnOverbroadModelPhraseBeforeItCanBeSaved(){
    UUID id=UUID.randomUUID();UUID record=UUID.randomUUID();
    when(jdbc.queryForList(anyString(),eq(id))).thenReturn(List.of(Map.of("record_id",record,"status","CONFIRMED")));
    when(jdbc.queryForList(anyString(),eq(record))).thenReturn(List.of(Map.of("content","I wish I would not wake up tomorrow")));
    when(model.configured()).thenReturn(true);
    when(model.suggestRiskPhrase(anyString())).thenReturn(new AiModelClient.RuleSuggestion("wake up","Too broad"));

    assertThatThrownBy(()->service.draft(id,UUID.randomUUID())).isInstanceOf(ApiException.class)
      .hasMessageContaining("short, generic first-person");
    verify(jdbc,never()).update(anyString(),any(Object[].class));
  }

  @Test void detectedGapCannotBeActivatedWithoutConfirmationAndDraftReview(){
    UUID id=UUID.randomUUID();UUID record=UUID.randomUUID();
    when(jdbc.queryForList(anyString(),eq(id))).thenReturn(List.of(Map.of("record_id",record,"status","DETECTED")));

    assertThatThrownBy(()->service.activate(id,UUID.randomUUID())).isInstanceOf(ApiException.class)
      .hasMessageContaining("expected state");
    verify(jdbc,never()).update(anyString(),any(Object[].class));
  }
}
