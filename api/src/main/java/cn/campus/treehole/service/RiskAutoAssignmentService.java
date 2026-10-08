package cn.campus.treehole.service;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service @RequiredArgsConstructor
public class RiskAutoAssignmentService {
  private final JdbcTemplate jdbc;

  @Scheduled(fixedDelayString="${app.risk.auto-assign-poll-ms:60000}")
  @Transactional
  public void assignOverdueCases(){
    var overdue=jdbc.queryForList("""
      select id,student_id from risk_cases
      where status='OPEN' and assignee_id is null and false_positive=false
        and created_at<=now()-interval '24 hours'
      order by case when risk_level='CRITICAL' then 0 else 1 end,created_at
      limit 20 for update skip locked
      """);
    for(var row:overdue){
      UUID teacherId=jdbc.query("""
        select u.id from users u left join risk_cases c on c.assignee_id=u.id
          and c.status in ('IN_PROGRESS','CLOSURE_PROPOSED')
        where u.role='TEACHER' and u.status='ACTIVE'
        group by u.id,u.created_at order by count(c.id),u.created_at,u.id limit 1
        """,rs->rs.next()?(UUID)rs.getObject(1):null);
      if(teacherId==null)break;
      UUID caseId=(UUID)row.get("id"),studentId=(UUID)row.get("student_id");
      int updated=jdbc.update("""
        update risk_cases set assignee_id=?,status='IN_PROGRESS',updated_at=now()
        where id=? and status='OPEN' and assignee_id is null and false_positive=false
        """,teacherId,caseId);
      if(updated==0)continue;
      jdbc.update("insert into risk_case_history(case_id,action,from_status,to_status,details) values (?,?,?,?,?)",caseId,"AUTO_ASSIGN_CASE","OPEN","IN_PROGRESS","Unclaimed for 24 hours; teacher="+teacherId);
      jdbc.update("insert into audit_logs(action,target_type,target_id,details) values (?,?,?,?)","AUTO_ASSIGN_CASE","RISK_CASE",caseId,"teacher="+teacherId);
      jdbc.update("insert into notifications(user_id,title,content,target_path) values (?,?,?,?)",teacherId,"Risk case assigned","A case remained unclaimed for 24 hours and was assigned to you.","/teacher?case="+caseId);
      jdbc.update("insert into notifications(user_id,title,content,target_path) values (?,?,?,?)",studentId,"A verified teacher joined your care case","You can now use the anonymous care conversation.","/care?case="+caseId);
    }
  }
}
