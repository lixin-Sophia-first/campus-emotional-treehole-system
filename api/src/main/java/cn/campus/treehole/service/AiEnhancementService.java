package cn.campus.treehole.service;

import cn.campus.treehole.domain.Enums.Visibility;
import cn.campus.treehole.repo.EmotionRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.UUID;

@Service @RequiredArgsConstructor
public class AiEnhancementService {
  private final EmotionRecordRepository records;
  private final RiskAnalysisService local;
  private final AiModelClient model;
  private final AiTaskProcessor processor;
  private final JdbcTemplate jdbc;

  @TransactionalEventListener(phase=TransactionPhase.AFTER_COMMIT)
  @Transactional(propagation=Propagation.REQUIRES_NEW)
  public void enqueue(EmotionRecordService.RecordCreated event){
    var record=records.findById(event.recordId()).orElse(null);
    if(record==null)return;
    var analysis=local.analyze(record.getContent());
    UUID taskId=UUID.randomUUID();
    String status=model.configured()&&record.getVisibility()==Visibility.PUBLIC&&record.getDeletedAt()==null
      ?"PENDING":"SKIPPED";
    jdbc.update("insert into ai_analysis_tasks(id,record_id,status) values (?,?,?)",taskId,record.getId(),status);
    jdbc.update("""
      insert into ai_analysis_results(task_id,record_id,source,primary_emotion,intensity,risk_level,
      explicit_plan,imminent_timing,means_present,violation_category,confidence,support_card,reason)
      values (?,?,?,?,?,?,?,?,?,?,?,?,?)
      """,taskId,record.getId(),"LOCAL",analysis.primaryEmotion(),analysis.intensity(),analysis.level().name(),
      analysis.explicitPlan(),analysis.imminentTiming(),analysis.meansPresent(),analysis.violationCategory(),
      analysis.confidence(),analysis.card(),String.join("; ",analysis.reasons()));
  }

  @Scheduled(fixedDelayString="${app.ai.poll-delay-ms:5000}")
  public void processQueue(){
    jdbc.update("""
      update ai_analysis_tasks t set status='SKIPPED',
        last_error='Private or deleted entry: local analysis only',updated_at=now()
      from emotion_records e where t.record_id=e.id
        and (e.visibility='PRIVATE' or e.deleted_at is not null)
        and t.status in ('PENDING','RUNNING','FAILED')
      """);
    if(!model.configured()){
      jdbc.update("""
        update ai_analysis_tasks set status='SKIPPED',
          last_error='External model not configured; local analysis remains active',updated_at=now()
        where status in ('PENDING','RUNNING')
        """);
      return;
    }
    jdbc.update("""
      update ai_analysis_tasks set status='PENDING',updated_at=now()
      where status='RUNNING' and updated_at<now()-interval '2 minutes'
      """);
    var tasks=jdbc.queryForList("""
      select t.id from ai_analysis_tasks t join emotion_records e on e.id=t.record_id
      where t.status='PENDING' and e.visibility='PUBLIC' and e.deleted_at is null
        and t.next_attempt_at<=now()
      order by t.created_at limit 5
      """);
    for(var task:tasks){
      UUID id=(UUID)task.get("id");
      try{processor.process(id);}catch(Exception failure){processor.markFailure(id,failure);}
    }
  }
}
