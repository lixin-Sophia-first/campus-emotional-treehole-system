package cn.campus.treehole.service;

import cn.campus.treehole.domain.Enums.*;
import cn.campus.treehole.repo.EmotionRecordRepository;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.web.client.RestClient;
import java.util.*;

@Service @RequiredArgsConstructor
public class AiEnhancementService {
  private final EmotionRecordRepository records; private final RiskCaseService cases; private final JsonMapper json;
  @Value("${app.ai.base-url:}") private String baseUrl;
  @Value("${app.ai.api-key:}") private String apiKey;
  @Value("${app.ai.model:}") private String model;
  @Async @TransactionalEventListener(phase=TransactionPhase.AFTER_COMMIT)
  @Transactional(propagation=Propagation.REQUIRES_NEW)
  public void enhance(EmotionRecordService.RecordCreated event){
    if(baseUrl.isBlank()||apiKey.isBlank()||model.isBlank()) return;
    var record=records.findById(event.recordId()).orElse(null); if(record==null) return;
    try {
      String system="You are a campus emotional-risk support classifier, not a diagnostic tool. Return JSON only: {riskLevel:LOW|MEDIUM|HIGH|CRITICAL,primaryEmotion:string,intensity:1-10,violation:boolean,supportCard:ENCOURAGEMENT|GROUNDING|SEEK_SUPPORT|URGENT_SUPPORT,reason:string}. An explicit plan, means, or imminent timing must be CRITICAL.";
      var body=Map.of("model",model,"temperature",0,"response_format",Map.of("type","json_object"),"messages",List.of(Map.of("role","system","content",system),Map.of("role","user","content",record.getContent())));
      JsonNode response=RestClient.create().post().uri(baseUrl.replaceAll("/$","")+"/chat/completions").header(HttpHeaders.AUTHORIZATION,"Bearer "+apiKey).contentType(MediaType.APPLICATION_JSON).body(body).retrieve().body(JsonNode.class);
      String raw=response.path("choices").path(0).path("message").path("content").asText().replace("```json","").replace("```","").trim();
      JsonNode result=json.readTree(raw); RiskLevel ai=RiskLevel.valueOf(result.path("riskLevel").asText("LOW"));
      if(ai.ordinal()>record.getRiskLevel().ordinal()) { record.setRiskLevel(ai); record.setRecommendationEligible(ai.ordinal()<RiskLevel.HIGH.ordinal()); record.setRiskReasons(record.getRiskReasons()+"; AI: "+result.path("reason").asText("risk upgraded")); record.setSupportCard(result.path("supportCard").asText(record.getSupportCard())); records.save(record); cases.ensureCase(record); }
      if(result.path("violation").asBoolean(false)) record.setModerationStatus(ModerationStatus.PENDING_REVIEW);
    } catch(Exception ignored) { /* Provider failures deliberately fall back to the local rules. */ }
  }
}
