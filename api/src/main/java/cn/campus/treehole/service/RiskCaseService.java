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
    if(record.getRiskLevel().ordinal()<RiskLevel.HIGH.ordinal()) return;
    var existing=cases.findByRecordId(record.getId());if(existing.isPresent()){var c=existing.get();if(record.getRiskLevel().ordinal()>c.getRiskLevel().ordinal()){c.setRiskLevel(record.getRiskLevel());jdbc.update("insert into risk_case_history(case_id,action,details) values (?,?,?)",c.getId(),"RISK_UPGRADED","Risk upgraded to "+record.getRiskLevel());if(record.getRiskLevel()==RiskLevel.CRITICAL)notifyRoles("Critical risk case upgraded","A risk case requires immediate attention");}if(record.getVisibility()==Visibility.PUBLIC)c.setPublicContentSnapshot(record.getContent());else c.setPublicContentSnapshot(null);return;}
    var c=new RiskCase(); c.setRecord(record); c.setStudent(record.getAuthor()); c.setRiskLevel(record.getRiskLevel()); c.setSourceVisibility(record.getVisibility());
    // Privacy invariant: private content is never copied into the case.
    c.setPublicContentSnapshot(record.getVisibility()==Visibility.PUBLIC ? record.getContent() : null);
    cases.save(c);jdbc.update("insert into risk_case_history(case_id,action,to_status,details) values (?,?,?,?)",c.getId(),"CASE_CREATED",c.getStatus().name(),"Created from "+record.getRiskLevel()+" risk signal");
    String title=record.getRiskLevel()==RiskLevel.CRITICAL?"Critical risk case detected":"High-risk case detected";
    notifyRoles(title,"A new anonymous risk case has entered the care queue");
  }
  private void notifyRoles(String title,String content){jdbc.update("insert into notifications(user_id,title,content) select id,?,? from users where role in ('ADMIN','TEACHER') and status='ACTIVE'",title,content);}
}
