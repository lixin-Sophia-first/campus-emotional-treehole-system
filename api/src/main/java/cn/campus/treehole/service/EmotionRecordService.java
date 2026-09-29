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
import java.time.Instant;
import java.util.*;
import static cn.campus.treehole.domain.Enums.*;

@Service @RequiredArgsConstructor
public class EmotionRecordService {
  private final EmotionRecordRepository records; private final RiskAnalysisService analyzer; private final RiskCaseService riskCases; private final ApplicationEventPublisher events;
  public record RecordCreated(UUID recordId) {}
  private static final String[] NAMES={"薄荷云","小海星","晚风","银杏叶","蒲公英","小月亮","青柠","萤火虫"};
  @Transactional public EmotionRecord create(User user,String content,Visibility visibility,String emotion,int intensity,String tags){
    var a=analyzer.analyze(content); var r=new EmotionRecord(); r.setAuthor(user); r.setContent(content.trim()); r.setVisibility(visibility); r.setEmotion(emotion); r.setIntensity(intensity); r.setTags(tags==null?"":tags.trim());
    r.setAnonymousAlias(NAMES[Math.floorMod(user.getId().hashCode()^content.hashCode(),NAMES.length)]); r.setRiskLevel(a.level()); r.setRiskReasons(String.join("；",a.reasons())); r.setSupportCard(a.card());
    r.setRecommendationEligible(a.level().ordinal()<RiskLevel.HIGH.ordinal());
    if(a.violation()) r.setModerationStatus(ModerationStatus.PENDING_REVIEW);
    records.save(r); riskCases.ensureCase(r); events.publishEvent(new RecordCreated(r.getId())); return r;
  }
  public Page<EmotionRecord> feed(boolean recommended,Pageable p){ return records.feed(recommended,p); }
  public Page<EmotionRecord> mine(UUID userId,Pageable p){ return records.findByAuthorIdAndDeletedAtIsNullOrderByCreatedAtDesc(userId,p); }
  public EmotionRecord visible(User viewer,UUID id){
    var r=records.findByIdAndDeletedAtIsNull(id).orElseThrow(()->new ApiException(HttpStatus.NOT_FOUND,"记录不存在"));
    if(r.getVisibility()==Visibility.PRIVATE && !r.getAuthor().getId().equals(viewer.getId())) throw new ApiException(HttpStatus.NOT_FOUND,"记录不存在");
    return r;
  }
  @Transactional public void delete(User user,UUID id){ var r=visible(user,id); if(!r.getAuthor().getId().equals(user.getId())) throw new ApiException(HttpStatus.FORBIDDEN,"只能删除自己的记录"); r.setDeletedAt(Instant.now()); r.setModerationStatus(ModerationStatus.DELETED); }
}
