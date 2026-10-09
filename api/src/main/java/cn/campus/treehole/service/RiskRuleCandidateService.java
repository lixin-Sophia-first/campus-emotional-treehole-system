package cn.campus.treehole.service;

import cn.campus.treehole.web.ApiErrorHandler.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service @RequiredArgsConstructor
public class RiskRuleCandidateService {
  private static final List<String> BENIGN_EXAMPLES=List.of(
    "I do not want to hurt myself",
    "I read an article about suicide prevention today",
    "I am going to class tonight",
    "I hope my friend feels better soon",
    "I want to sleep after a long exam",
    "My essay is about mental health support"
  );
  private final JdbcTemplate jdbc;
  private final AiModelClient model;
  private final RiskAnalysisService local;

  public List<Map<String,Object>> list(){
    return jdbc.queryForList("""
      select c.id,c.version,c.task_id,c.record_id,c.local_risk,c.model_risk,c.status,
             c.phrase,c.rationale,c.created_at,c.reviewed_at,c.activated_at,c.disabled_at,
             (select r.reason from ai_analysis_results r where r.task_id=c.task_id and r.source='MODEL' order by r.created_at desc limit 1) model_reason,
             e.title,e.content
      from risk_rule_candidates c join emotion_records e on e.id=c.record_id
      where e.visibility='PUBLIC' and e.deleted_at is null
      order by c.created_at desc limit 200
      """);
  }

  @Transactional public void confirm(UUID id,UUID actor){
    var row=requireStatus(id,"DETECTED");
    Integer cases=jdbc.queryForObject("select count(*) from risk_cases where record_id=? and false_positive=false",Integer.class,row.get("record_id"));
    if(cases==null||cases==0)throw new ApiException(HttpStatus.CONFLICT,"Confirm the related risk case before learning a rule from it.");
    if(jdbc.update("update risk_rule_candidates set status='CONFIRMED',reviewed_by=?,reviewed_at=now(),updated_at=now() where id=? and status='DETECTED'",actor,id)!=1)
      throw conflict();
    audit(actor,"CONFIRM_MODEL_RULE_GAP",id);
  }

  @Transactional public void reject(UUID id,UUID actor){
    requireStatus(id,"DETECTED","CONFIRMED","DRAFTED");
    if(jdbc.update("update risk_rule_candidates set status='REJECTED',reviewed_by=?,reviewed_at=now(),updated_at=now() where id=? and status in ('DETECTED','CONFIRMED','DRAFTED')",actor,id)!=1)
      throw conflict();
    audit(actor,"REJECT_MODEL_RULE_GAP",id);
  }

  public void draft(UUID id,UUID actor){
    var row=requireStatus(id,"CONFIRMED");
    if(!model.configured())throw new ApiException(HttpStatus.CONFLICT,"External AI is not configured; no rule can be drafted.");
    UUID recordId=(UUID)row.get("record_id");
    var source=jdbc.queryForList("select content from emotion_records where id=? and visibility='PUBLIC' and deleted_at is null",recordId);
    if(source.isEmpty())throw new ApiException(HttpStatus.CONFLICT,"The source is no longer a public post.");
    String content=(String)source.getFirst().get("content");
    AiModelClient.RuleSuggestion suggestion;
    try{suggestion=model.suggestRiskPhrase(content);}
    catch(Exception e){throw new ApiException(HttpStatus.BAD_GATEWAY,"The model could not draft a valid rule. The confirmed case is unchanged; please try again later.");}
    String phrase=normalizeAndValidate(suggestion.phrase(),content);
    if(jdbc.update("update risk_rule_candidates set status='DRAFTED',phrase=?,rationale=?,updated_at=now() where id=? and status='CONFIRMED'",
      phrase,suggestion.rationale(),id)!=1)throw conflict();
    audit(actor,"DRAFT_RISK_RULE",id);
  }

  @Transactional public void activate(UUID id,UUID actor){
    var row=requireStatus(id,"DRAFTED");
    var source=jdbc.queryForList("select content from emotion_records where id=? and visibility='PUBLIC' and deleted_at is null",row.get("record_id"));
    if(source.isEmpty())throw new ApiException(HttpStatus.CONFLICT,"The source is no longer a public post.");
    String phrase=normalizeAndValidate((String)row.get("phrase"),(String)source.getFirst().get("content"));
    if(BENIGN_EXAMPLES.stream().anyMatch(example->local.matchesReviewedPhrase(example,phrase)))
      throw new ApiException(HttpStatus.CONFLICT,"This rule also matches a benign regression example. Keep it inactive.");
    Integer duplicates=jdbc.queryForObject("select count(*) from risk_rule_candidates where status='ACTIVE' and lower(phrase)=?",Integer.class,phrase);
    if(duplicates!=null&&duplicates>0)throw new ApiException(HttpStatus.CONFLICT,"This phrase is already active.");
    if(jdbc.update("update risk_rule_candidates set status='ACTIVE',reviewed_by=?,activated_at=now(),updated_at=now() where id=? and status='DRAFTED'",actor,id)!=1)
      throw conflict();
    audit(actor,"ACTIVATE_RISK_RULE",id);
  }

  @Transactional public void disable(UUID id,UUID actor){
    requireStatus(id,"ACTIVE");
    if(jdbc.update("update risk_rule_candidates set status='DISABLED',disabled_at=now(),updated_at=now() where id=? and status='ACTIVE'",id)!=1)
      throw conflict();
    audit(actor,"DISABLE_RISK_RULE",id);
  }

  private String normalizeAndValidate(String raw,String source){
    String phrase=raw.toLowerCase(Locale.ROOT).replace('’','\'').replaceAll("\\s+"," ").trim();
    int words=phrase.split(" ").length;
    if(phrase.length()<12||phrase.length()>120||words<3||words>12
      ||!phrase.matches("[a-z][a-z' -]*")
      ||!phrase.matches(".*\\b(i|i'm|my|me|myself)\\b.*")
      ||!local.matchesReviewedPhrase(source,phrase))
      throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY,"The suggested phrase must be a short, generic first-person expression found in the public post. Ask the model to draft again.");
    return phrase;
  }

  private Map<String,Object> requireStatus(UUID id,String... allowed){
    var rows=jdbc.queryForList("""
      select c.record_id,c.status,c.phrase from risk_rule_candidates c
      join emotion_records e on e.id=c.record_id
      where c.id=? and e.visibility='PUBLIC' and e.deleted_at is null
      """,id);
    if(rows.isEmpty())throw new ApiException(HttpStatus.NOT_FOUND,"Rule candidate not found");
    var row=rows.getFirst();
    if(!List.of(allowed).contains(row.get("status")))throw conflict();
    return row;
  }
  private ApiException conflict(){return new ApiException(HttpStatus.CONFLICT,"This rule candidate is no longer in the expected state. Refresh the page.");}
  private void audit(UUID actor,String action,UUID target){
    jdbc.update("insert into audit_logs(actor_id,action,target_type,target_id,details) values (?,?,?,?,?)",actor,action,"RISK_RULE_CANDIDATE",target,"");
  }
}
