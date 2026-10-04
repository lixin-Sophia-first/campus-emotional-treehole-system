package cn.campus.treehole.service;

import cn.campus.treehole.domain.Enums.*;
import cn.campus.treehole.repo.EmotionRecordRepository;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.web.client.RestClient;
import java.time.Duration;
import java.util.*;

@Service @RequiredArgsConstructor
public class AiEnhancementService {
  private final EmotionRecordRepository records; private final RiskCaseService cases; private final RiskAnalysisService local; private final JsonMapper json; private final JdbcTemplate jdbc;
  @Value("${app.ai.base-url:}") private String baseUrl; @Value("${app.ai.api-key:}") private String apiKey; @Value("${app.ai.model:}") private String model;

  @TransactionalEventListener(phase=TransactionPhase.AFTER_COMMIT) @Transactional(propagation=Propagation.REQUIRES_NEW)
  public void enqueue(EmotionRecordService.RecordCreated event){var record=records.findById(event.recordId()).orElse(null);if(record==null)return;var a=local.analyze(record.getContent());UUID task=UUID.randomUUID();String status=configured()?"PENDING":"SKIPPED";jdbc.update("insert into ai_analysis_tasks(id,record_id,status) values (?,?,?)",task,record.getId(),status);saveResult(task,record.getId(),"LOCAL",a.primaryEmotion(),a.intensity(),a.level(),a.explicitPlan(),a.imminentTiming(),a.meansPresent(),a.violationCategory(),a.confidence(),a.card(),String.join("; ",a.reasons()));}

  @Scheduled(fixedDelayString="${app.ai.poll-delay-ms:5000}") @Transactional
  public void processQueue(){if(!configured())return;var tasks=jdbc.queryForList("select id,record_id from ai_analysis_tasks where status='PENDING' and next_attempt_at<=now() order by created_at limit 5 for update skip locked");for(var row:tasks){UUID task=(UUID)row.get("id"),record=(UUID)row.get("record_id");jdbc.update("update ai_analysis_tasks set status='RUNNING',attempts=attempts+1,updated_at=now() where id=?",task);try{analyze(task,record);jdbc.update("update ai_analysis_tasks set status='SUCCEEDED',last_error='',updated_at=now() where id=?",task);}catch(Exception e){Integer attempts=jdbc.queryForObject("select attempts from ai_analysis_tasks where id=?",Integer.class,task);String status=attempts!=null&&attempts>=3?"FAILED":"PENDING";String error=Optional.ofNullable(e.getMessage()).orElse(e.getClass().getSimpleName());if(error.length()>500)error=error.substring(0,500);jdbc.update("update ai_analysis_tasks set status=?,last_error=?,next_attempt_at=now()+(attempts*interval '30 seconds'),updated_at=now() where id=?",status,error,task);}}}

  private void analyze(UUID taskId,UUID recordId)throws Exception{var record=records.findById(recordId).orElseThrow();boolean alreadyPending=record.getModerationStatus()==ModerationStatus.PENDING_REVIEW;String system="You are an English-only campus emotional-risk classifier, not a diagnostic tool. Return JSON only with: riskLevel LOW|MEDIUM|HIGH|CRITICAL, primaryEmotion, intensity 1-10, explicitPlan boolean, imminentTiming boolean, meansPresent boolean, violationCategory NONE|HARASSMENT|SEXUAL_CONTENT|ILLEGAL_CONTENT|SPAM, confidence 0-1, supportCard ENCOURAGEMENT|GROUNDING|SEEK_SUPPORT|URGENT_SUPPORT, reason. Explicit plan plus timing or means must be CRITICAL.";var body=Map.of("model",model,"temperature",0,"response_format",Map.of("type","json_object"),"messages",List.of(Map.of("role","system","content",system),Map.of("role","user","content",record.getContent())));var factory=new SimpleClientHttpRequestFactory();factory.setConnectTimeout(Duration.ofSeconds(5));factory.setReadTimeout(Duration.ofSeconds(15));JsonNode response=RestClient.builder().requestFactory(factory).build().post().uri(baseUrl.replaceAll("/$","")+"/chat/completions").header(HttpHeaders.AUTHORIZATION,"Bearer "+apiKey).contentType(MediaType.APPLICATION_JSON).body(body).retrieve().body(JsonNode.class);String raw=response.path("choices").path(0).path("message").path("content").asText().replace("```json","").replace("```","").trim();JsonNode r=json.readTree(raw);RiskLevel ai=RiskLevel.valueOf(r.path("riskLevel").asText("LOW"));boolean plan=r.path("explicitPlan").asBoolean(false),timing=r.path("imminentTiming").asBoolean(false),means=r.path("meansPresent").asBoolean(false);if(plan&&(timing||means))ai=RiskLevel.CRITICAL;String violation=r.path("violationCategory").asText("NONE");String card=r.path("supportCard").asText("ENCOURAGEMENT");saveResult(taskId,recordId,"MODEL",r.path("primaryEmotion").asText("Unspecified"),Math.max(1,Math.min(10,r.path("intensity").asInt(record.getIntensity()))),ai,plan,timing,means,violation,Math.max(0,Math.min(1,r.path("confidence").asDouble(0))),card,r.path("reason").asText(""));if(ai.ordinal()>record.getRiskLevel().ordinal()){record.setRiskLevel(ai);record.setRecommendationEligible(ai.ordinal()<RiskLevel.HIGH.ordinal());record.setRiskReasons(record.getRiskReasons()+"; AI: "+r.path("reason").asText("risk upgraded"));record.setSupportCard(card);}if(!"NONE".equals(violation)){record.setViolationCategory(violation);record.setModerationStatus(ModerationStatus.PENDING_REVIEW);}if(record.getVisibility()==Visibility.PUBLIC&&record.getRiskLevel().ordinal()>=RiskLevel.HIGH.ordinal())record.setModerationStatus(ModerationStatus.PENDING_REVIEW);records.save(record);if(!alreadyPending&&record.getModerationStatus()==ModerationStatus.PENDING_REVIEW)jdbc.update("insert into notifications(user_id,title,content) values (?,?,?)",record.getAuthor().getId(),"Post awaiting review","Further analysis marked your post for administrator review. It is not public while the review is pending.");cases.ensureCase(record);}
  private void saveResult(UUID task,UUID record,String source,String emotion,int intensity,RiskLevel risk,boolean plan,boolean timing,boolean means,String violation,double confidence,String card,String reason){jdbc.update("insert into ai_analysis_results(task_id,record_id,source,primary_emotion,intensity,risk_level,explicit_plan,imminent_timing,means_present,violation_category,confidence,support_card,reason) values (?,?,?,?,?,?,?,?,?,?,?,?,?)",task,record,source,emotion,intensity,risk.name(),plan,timing,means,violation,confidence,card,reason==null?"":reason);}
  private boolean configured(){return !baseUrl.isBlank()&&!apiKey.isBlank()&&!model.isBlank();}
}
