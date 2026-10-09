package cn.campus.treehole.service;

import cn.campus.treehole.domain.EmotionRecord;
import cn.campus.treehole.domain.Enums.*;
import cn.campus.treehole.repo.EmotionRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service @RequiredArgsConstructor
public class AiTaskProcessor {
  private final JdbcTemplate jdbc;
  private final EmotionRecordRepository records;
  private final RiskCaseService cases;
  private final AiModelClient model;

  @Transactional
  public void process(UUID taskId){
    var task=jdbc.queryForList("select record_id,status from ai_analysis_tasks where id=? for update",taskId);
    if(task.isEmpty()||!"PENDING".equals(task.getFirst().get("status")))return;
    UUID recordId=(UUID)task.getFirst().get("record_id");
    var access=jdbc.queryForList("select visibility,deleted_at from emotion_records where id=? for update",recordId);
    if(access.isEmpty()||!"PUBLIC".equals(access.getFirst().get("visibility"))||access.getFirst().get("deleted_at")!=null){
      jdbc.update("update ai_analysis_tasks set status='SKIPPED',last_error='Private or deleted entry: local analysis only',updated_at=now() where id=?",taskId);
      return;
    }
    EmotionRecord record=records.findById(recordId).orElseThrow();
    AiModelClient.Assessment result=model.analyzePublicText(record.getContent());
    var localResults=jdbc.queryForList("select risk_level from ai_analysis_results where task_id=? and source='LOCAL' order by created_at desc limit 1",taskId);
    RiskLevel localRisk=localResults.isEmpty()?record.getRiskLevel():RiskLevel.valueOf((String)localResults.getFirst().get("risk_level"));
    RiskLevel finalRisk=record.getRiskLevel().ordinal()>result.riskLevel().ordinal()?record.getRiskLevel():result.riskLevel();
    jdbc.update("""
      insert into ai_analysis_results(task_id,record_id,source,primary_emotion,intensity,risk_level,
      explicit_plan,imminent_timing,means_present,violation_category,confidence,support_card,reason)
      values (?,?,?,?,?,?,?,?,?,?,?,?,?)
      """,taskId,recordId,"MODEL",result.primaryEmotion(),result.intensity(),result.riskLevel().name(),
      result.explicitPlan(),result.imminentTiming(),result.meansPresent(),result.violationCategory(),
      result.confidence(),result.supportCard(),result.reason());
    if(localRisk.ordinal()<RiskLevel.HIGH.ordinal()&&result.riskLevel().ordinal()>=RiskLevel.HIGH.ordinal())
      jdbc.update("""
        insert into risk_rule_candidates(task_id,record_id,local_risk,model_risk)
        select ?,?,?,? where not exists
          (select 1 from risk_cases where record_id=? and false_positive=true)
        on conflict (task_id) do nothing
        """,taskId,recordId,localRisk.name(),result.riskLevel().name(),recordId);
    boolean wasPending=record.getModerationStatus()==ModerationStatus.PENDING_REVIEW;
    if(finalRisk.ordinal()>record.getRiskLevel().ordinal()){
      record.setRiskLevel(finalRisk);
      record.setRiskReasons(record.getRiskReasons()+"; AI: "+result.reason());
    }
    record.setRecommendationEligible(finalRisk.ordinal()<RiskLevel.HIGH.ordinal());
    record.setSupportCard(cardFor(finalRisk));
    if(!"NONE".equals(result.violationCategory())){
      record.setViolationCategory(result.violationCategory());
      record.setModerationStatus(ModerationStatus.PENDING_REVIEW);
    }
    if(finalRisk.ordinal()>=RiskLevel.HIGH.ordinal())record.setModerationStatus(ModerationStatus.PENDING_REVIEW);
    records.save(record);
    if(!wasPending&&record.getModerationStatus()==ModerationStatus.PENDING_REVIEW)
      jdbc.update("insert into notifications(user_id,title,content,target_path) values (?,?,?,?)",record.getAuthor().getId(),
        "Post awaiting review","Further analysis marked your post for administrator review. It is not public while the review is pending.","/records/"+recordId);
    cases.ensureCase(record);
    jdbc.update("update ai_analysis_tasks set status='SUCCEEDED',attempts=attempts+1,last_error='',updated_at=now() where id=?",taskId);
  }

  @Transactional
  public void markFailure(UUID taskId,Exception failure){
    var task=jdbc.queryForList("select attempts,status from ai_analysis_tasks where id=? for update",taskId);
    if(task.isEmpty()||!"PENDING".equals(task.getFirst().get("status")))return;
    int attempts=((Number)task.getFirst().get("attempts")).intValue()+1;
    String status=attempts>=3?"FAILED":"PENDING";
    String reason=failure instanceof IllegalArgumentException?"Model response was invalid":"Model service was unavailable";
    jdbc.update("""
      update ai_analysis_tasks set status=?,attempts=?,last_error=?,
        next_attempt_at=now()+(? * interval '30 seconds'),updated_at=now() where id=?
      """,status,attempts,reason,attempts,taskId);
  }

  private String cardFor(RiskLevel level){return switch(level){
    case LOW->"ENCOURAGEMENT";case MEDIUM->"GROUNDING";
    case HIGH->"SEEK_SUPPORT";case CRITICAL->"URGENT_SUPPORT";
  };}
}
