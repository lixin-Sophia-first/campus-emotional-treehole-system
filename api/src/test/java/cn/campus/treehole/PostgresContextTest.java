package cn.campus.treehole;

import cn.campus.treehole.service.RiskAutoAssignmentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest(properties={"spring.mail.host=localhost","spring.mail.port=1025","app.risk.auto-assign-poll-ms=3600000"})
class PostgresContextTest {
  @Container @ServiceConnection
  static PostgreSQLContainer<?> postgres=new PostgreSQLContainer<>("postgres:17-alpine");
  @Autowired JdbcTemplate jdbc;
  @Autowired RiskAutoAssignmentService autoAssignment;

  @Test void applicationStartsWithRealPostgres(){assertThat(postgres.isRunning()).isTrue();}

  @Test @Transactional void onlyCasesUnclaimedForAtLeast24HoursAreAutoAssigned(){
    var teacher=java.util.UUID.randomUUID();
    var student=java.util.UUID.randomUUID();
    var oldRecord=java.util.UUID.randomUUID();
    var freshRecord=java.util.UUID.randomUUID();
    var oldCase=java.util.UUID.randomUUID();
    var freshCase=java.util.UUID.randomUUID();
    jdbc.update("insert into users(id,email,password_hash,display_name,role,status) values (?,?,?,?,?,?)",teacher,"auto-teacher-"+teacher+"@example.test","test","Teacher","TEACHER","ACTIVE");
    jdbc.update("insert into users(id,email,password_hash,display_name,role,status) values (?,?,?,?,?,?)",student,"auto-student-"+student+"@example.test","test","Student","STUDENT","ACTIVE");
    for(var record:java.util.List.of(oldRecord,freshRecord))
      jdbc.update("insert into emotion_records(id,author_id,title,content,visibility,emotion,intensity,anonymous_alias,risk_level) values (?,?,?,?,?,?,?,?,?)",record,student,"Test","Test content","PRIVATE","Sad",8,"Stranger","HIGH");
    jdbc.update("insert into risk_cases(id,record_id,student_id,risk_level,source_visibility,created_at) values (?,?,?,?,?,now()-interval '25 hours')",oldCase,oldRecord,student,"HIGH","PRIVATE");
    jdbc.update("insert into risk_cases(id,record_id,student_id,risk_level,source_visibility,created_at) values (?,?,?,?,?,now()-interval '23 hours')",freshCase,freshRecord,student,"HIGH","PRIVATE");

    autoAssignment.assignOverdueCases();

    assertThat(jdbc.queryForObject("select assignee_id from risk_cases where id=?",java.util.UUID.class,oldCase)).isEqualTo(teacher);
    assertThat(jdbc.queryForObject("select status from risk_cases where id=?",String.class,oldCase)).isEqualTo("IN_PROGRESS");
    assertThat(jdbc.queryForObject("select assignee_id from risk_cases where id=?",java.util.UUID.class,freshCase)).isNull();
    assertThat(jdbc.queryForObject("select count(*) from risk_case_history where case_id=? and action='AUTO_ASSIGN_CASE'",Long.class,oldCase)).isEqualTo(1L);
  }
}
