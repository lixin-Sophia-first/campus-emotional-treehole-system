package cn.campus.treehole.service;

import cn.campus.treehole.domain.EmotionRecord;
import cn.campus.treehole.domain.Enums.Visibility;
import cn.campus.treehole.repo.EmotionRecordRepository;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.json.JsonMapper;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AiEnhancementServiceTest {
  @Test void privateEntryNeverQueuesExternalAnalysisEvenWhenModelIsConfigured(){
    var records=mock(EmotionRecordRepository.class);
    var cases=mock(RiskCaseService.class);
    var jdbc=mock(JdbcTemplate.class);
    var service=new AiEnhancementService(records,cases,new RiskAnalysisService(),mock(JsonMapper.class),jdbc);
    ReflectionTestUtils.setField(service,"baseUrl","https://model.example.test");
    ReflectionTestUtils.setField(service,"apiKey","test-key");
    ReflectionTestUtils.setField(service,"model","test-model");
    UUID id=UUID.randomUUID();
    var record=new EmotionRecord();record.setId(id);record.setVisibility(Visibility.PRIVATE);record.setContent("I feel worried today");
    when(records.findById(id)).thenReturn(Optional.of(record));

    service.enqueue(new EmotionRecordService.RecordCreated(id));

    verify(jdbc).update(contains("insert into ai_analysis_tasks"),any(UUID.class),eq(id),eq("SKIPPED"));
    verifyNoInteractions(cases);
  }
}
