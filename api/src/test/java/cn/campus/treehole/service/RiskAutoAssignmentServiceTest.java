package cn.campus.treehole.service;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class RiskAutoAssignmentServiceTest {
  @Test void assignsOverdueUnclaimedCaseToActiveLeastLoadedTeacher(){
    var jdbc=mock(JdbcTemplate.class);
    UUID caseId=UUID.randomUUID(),studentId=UUID.randomUUID(),teacherId=UUID.randomUUID();
    when(jdbc.queryForList(contains("created_at<=now()-interval '24 hours'")))
      .thenReturn(List.of(Map.of("id",caseId,"student_id",studentId)));
    when(jdbc.query(anyString(),any(ResultSetExtractor.class))).thenReturn(teacherId);
    when(jdbc.update(startsWith("update risk_cases"),eq(teacherId),eq(caseId))).thenReturn(1);

    new RiskAutoAssignmentService(jdbc).assignOverdueCases();

    verify(jdbc).update(startsWith("update risk_cases"),eq(teacherId),eq(caseId));
    verify(jdbc).update(contains("risk_case_history"),eq(caseId),eq("AUTO_ASSIGN_CASE"),eq("OPEN"),eq("IN_PROGRESS"),anyString());
    verify(jdbc).update(contains("audit_logs"),eq("AUTO_ASSIGN_CASE"),eq("RISK_CASE"),eq(caseId),anyString());
    verify(jdbc).update(contains("notifications"),eq(teacherId),anyString(),anyString(),anyString());
    verify(jdbc).update(contains("notifications"),eq(studentId),anyString(),anyString(),anyString());
  }

  @Test void leavesCaseUnassignedWhenNoActiveTeacherExists(){
    var jdbc=mock(JdbcTemplate.class);
    when(jdbc.queryForList(anyString())).thenReturn(List.of(Map.of("id",UUID.randomUUID(),"student_id",UUID.randomUUID())));
    new RiskAutoAssignmentService(jdbc).assignOverdueCases();
    verify(jdbc,never()).update(anyString(),any(Object[].class));
  }
}
