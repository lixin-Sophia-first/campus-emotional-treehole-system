package cn.campus.treehole.service;

import cn.campus.treehole.domain.*;
import cn.campus.treehole.repo.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.jdbc.core.JdbcTemplate;
import java.util.*;
import static cn.campus.treehole.domain.Enums.*;

@Service @RequiredArgsConstructor
public class RiskCaseService {
  private final RiskCaseRepository cases; private final JdbcTemplate jdbc;
  @Transactional public void ensureCase(EmotionRecord record){
    if(record.getRiskLevel().ordinal()<RiskLevel.HIGH.ordinal() || cases.findByRecordId(record.getId()).isPresent()) return;
    var c=new RiskCase(); c.setRecord(record); c.setStudent(record.getAuthor()); c.setRiskLevel(record.getRiskLevel()); c.setSourceVisibility(record.getVisibility());
    // Privacy invariant: private content is never copied into the case.
    c.setPublicContentSnapshot(record.getVisibility()==Visibility.PUBLIC ? record.getContent() : null);
    cases.save(c);
    String title=record.getRiskLevel()==RiskLevel.CRITICAL?"Critical risk case detected":"High-risk case detected";
    jdbc.update("insert into notifications(user_id,title,content) select id,?,? from users where role in ('ADMIN','TEACHER') and status='ACTIVE'",title,"A new anonymous risk case has entered the care queue");
  }
}
