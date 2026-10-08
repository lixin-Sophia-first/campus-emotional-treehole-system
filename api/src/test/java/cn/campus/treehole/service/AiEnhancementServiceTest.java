package cn.campus.treehole.service;

import cn.campus.treehole.domain.EmotionRecord;
import cn.campus.treehole.domain.Enums.Visibility;
import cn.campus.treehole.repo.EmotionRecordRepository;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AiEnhancementServiceTest {
  @Test void privateEntryNeverQueuesExternalAnalysisEvenWhenModelIsConfigured(){
    var records=mock(EmotionRecordRepository.class);
    var model=mock(AiModelClient.class);
    var processor=mock(AiTaskProcessor.class);
    var jdbc=mock(JdbcTemplate.class);
    var service=new AiEnhancementService(records,new RiskAnalysisService(),model,processor,jdbc);
    when(model.configured()).thenReturn(true);
    UUID id=UUID.randomUUID();
    var record=new EmotionRecord();record.setId(id);record.setVisibility(Visibility.PRIVATE);record.setContent("I feel worried today");
    when(records.findById(id)).thenReturn(Optional.of(record));

    service.enqueue(new EmotionRecordService.RecordCreated(id));

    verify(jdbc).update(contains("insert into ai_analysis_tasks"),any(UUID.class),eq(id),eq("SKIPPED"));
    verifyNoInteractions(processor);
  }

  @Test void queuedModelWorkBecomesLocalOnlyWhenEndpointIsDisabled(){
    var jdbc=mock(JdbcTemplate.class);
    var model=mock(AiModelClient.class);
    var processor=mock(AiTaskProcessor.class);
    new AiEnhancementService(mock(EmotionRecordRepository.class),new RiskAnalysisService(),model,processor,jdbc).processQueue();
    verify(jdbc).update(contains("Private or deleted entry: local analysis only"));
    verify(jdbc).update(contains("External model not configured; local analysis remains active"));
    verifyNoInteractions(processor);
  }
}
