package cn.campus.treehole.service;

import cn.campus.treehole.domain.*;
import cn.campus.treehole.repo.EmotionRecordRepository;
import cn.campus.treehole.web.ApiErrorHandler.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import java.time.Instant;
import java.util.*;
import static cn.campus.treehole.domain.Enums.*;

@Service @RequiredArgsConstructor
public class EmotionRecordService {
  private final EmotionRecordRepository records; private final RiskAnalysisService analyzer; private final RiskCaseService riskCases; private final ApplicationEventPublisher events; private final JdbcTemplate jdbc;
  public record RecordCreated(UUID recordId) {}
  @Transactional public EmotionRecord create(User user,String title,String content,Visibility visibility,String emotion,int intensity,String tags,boolean anonymous){
    var a=analyzer.analyze(content); var r=new EmotionRecord(); r.setAuthor(user); r.setTitle(title.trim()); r.setContent(content.trim()); r.setVisibility(visibility); r.setEmotion(emotion); r.setIntensity(intensity); r.setTags(tags==null?"":tags.trim());
    r.setAnonymous(anonymous);r.setAnonymousAlias(anonymous?"Stranger":user.getDisplayName()); r.setRiskLevel(a.level()); r.setRiskReasons(String.join("; ",a.reasons())); r.setSupportCard(a.card());
    r.setRecommendationEligible(a.level().ordinal()<RiskLevel.HIGH.ordinal());
    r.setViolationCategory(a.violationCategory()); if(a.violation()||(visibility==Visibility.PUBLIC&&a.level().ordinal()>=RiskLevel.HIGH.ordinal())) r.setModerationStatus(ModerationStatus.PENDING_REVIEW);
    records.saveAndFlush(r); riskCases.ensureCase(r); if(visibility==Visibility.PUBLIC&&r.getModerationStatus()==ModerationStatus.PENDING_REVIEW)jdbc.update("insert into notifications(user_id,title,content) values (?,?,?)",user.getId(),"Post awaiting review","Your post was saved but is not public yet. An administrator will review it; you can follow its status in Notifications."); events.publishEvent(new RecordCreated(r.getId())); return r;
  }
  public Page<EmotionRecord> feed(String mode,String query,String tag,Pageable p){ if((query!=null&&!query.isBlank())||(tag!=null&&!tag.isBlank()))return records.search(query==null?"":query.trim(),tag==null?"":tag.trim(),p); return switch(mode){case "latest"->records.latest(p);case "hot"->records.hot(p);default->records.recommended(p);}; }
  public Page<EmotionRecord> mine(UUID userId,Pageable p){ return records.findByAuthorIdAndDeletedAtIsNullOrderByCreatedAtDesc(userId,p); }
  public EmotionRecord visible(User viewer,UUID id){
    var r=records.findByIdAndDeletedAtIsNull(id).orElseThrow(()->new ApiException(HttpStatus.NOT_FOUND,"Entry not found"));
    if(r.getVisibility()==Visibility.PRIVATE && (viewer==null||!r.getAuthor().getId().equals(viewer.getId()))) throw new ApiException(HttpStatus.NOT_FOUND,"Entry not found");
    if(r.getVisibility()==Visibility.PUBLIC&&r.getModerationStatus()!=ModerationStatus.PUBLISHED&&(viewer==null||!r.getAuthor().getId().equals(viewer.getId()))) throw new ApiException(HttpStatus.NOT_FOUND,"Entry not found");
    return r;
  }
  @Transactional public EmotionRecord update(User user,UUID id,String title,String content,Visibility visibility,String emotion,int intensity,String tags,boolean anonymous){var r=visible(user,id);if(!r.getAuthor().getId().equals(user.getId()))throw new ApiException(HttpStatus.FORBIDDEN,"You can only edit your own entries");var a=analyzer.analyze(content);r.setTitle(title.trim());r.setContent(content.trim());r.setVisibility(visibility);r.setEmotion(emotion);r.setIntensity(intensity);r.setTags(tags==null?"":tags.trim());r.setAnonymous(anonymous);r.setAnonymousAlias(anonymous?"Stranger":user.getDisplayName());if(a.level().ordinal()>r.getRiskLevel().ordinal())r.setRiskLevel(a.level());r.setRiskReasons(String.join("; ",a.reasons()));r.setSupportCard(a.card());r.setRecommendationEligible(r.getRiskLevel().ordinal()<RiskLevel.HIGH.ordinal());r.setViolationCategory(a.violationCategory());if(a.violation()||(visibility==Visibility.PUBLIC&&r.getRiskLevel().ordinal()>=RiskLevel.HIGH.ordinal()))r.setModerationStatus(ModerationStatus.PENDING_REVIEW);records.save(r);riskCases.ensureCase(r);events.publishEvent(new RecordCreated(r.getId()));return r;}
  @Transactional public void delete(User user,UUID id){ var r=visible(user,id); if(!r.getAuthor().getId().equals(user.getId())) throw new ApiException(HttpStatus.FORBIDDEN,"You can only delete your own entries"); r.setDeletedAt(Instant.now()); r.setModerationStatus(ModerationStatus.DELETED); }
}
