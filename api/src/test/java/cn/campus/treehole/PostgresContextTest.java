package cn.campus.treehole;

import cn.campus.treehole.service.RiskAutoAssignmentService;
import cn.campus.treehole.service.AiModelClient;
import cn.campus.treehole.service.AiTaskProcessor;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.util.ReflectionTestUtils;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest(properties={"spring.mail.host=localhost","spring.mail.port=1025","app.risk.auto-assign-poll-ms=3600000","app.ai.poll-delay-ms=3600000"})
class PostgresContextTest {
  @Container @ServiceConnection
  static PostgreSQLContainer<?> postgres=new PostgreSQLContainer<>("postgres:17-alpine");
  @Autowired JdbcTemplate jdbc;
  @Autowired RiskAutoAssignmentService autoAssignment;
  @Autowired AiTaskProcessor aiProcessor;
  @Autowired AiModelClient aiModel;

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

  @Test @Transactional void privateEntryTaskIsSkippedWithoutCallingExternalModel(){
    var student=java.util.UUID.randomUUID();var record=java.util.UUID.randomUUID();var task=java.util.UUID.randomUUID();
    insertStudentAndRecord(student,record,"PRIVATE");
    jdbc.update("insert into ai_analysis_tasks(id,record_id,status) values (?,?,?)",task,record,"PENDING");
    ReflectionTestUtils.setField(aiModel,"baseUrl","http://127.0.0.1:1/v1");
    ReflectionTestUtils.setField(aiModel,"apiKey","test-key");
    ReflectionTestUtils.setField(aiModel,"model","mock-model");
    try{
      aiProcessor.process(task);
      assertThat(jdbc.queryForObject("select status from ai_analysis_tasks where id=?",String.class,task)).isEqualTo("SKIPPED");
      assertThat(jdbc.queryForObject("select count(*) from ai_analysis_results where task_id=? and source='MODEL'",Long.class,task)).isZero();
    }finally{clearModelConfiguration();}
  }

  @Test @Transactional void validModelResultUpgradesPublicRiskAndCreatesCase() throws Exception {
    var student=java.util.UUID.randomUUID();var record=java.util.UUID.randomUUID();var task=java.util.UUID.randomUUID();
    insertStudentAndRecord(student,record,"PUBLIC");
    jdbc.update("insert into ai_analysis_tasks(id,record_id,status) values (?,?,?)",task,record,"PENDING");
    var server=HttpServer.create(new java.net.InetSocketAddress("127.0.0.1",0),0);
    server.createContext("/v1/chat/completions",exchange->{
      String modelResult="{\"riskLevel\":\"CRITICAL\",\"primaryEmotion\":\"Sad\",\"intensity\":10,\"explicitPlan\":true,\"imminentTiming\":true,\"meansPresent\":false,\"violationCategory\":\"NONE\",\"confidence\":0.94,\"supportCard\":\"URGENT_SUPPORT\",\"reason\":\"Strong risk signal\"}";
      byte[] response=tools.jackson.databind.json.JsonMapper.builder().build().writeValueAsBytes(java.util.Map.of("choices",new Object[]{java.util.Map.of("message",java.util.Map.of("content",modelResult))}));
      exchange.getResponseHeaders().set("Content-Type","application/json");exchange.sendResponseHeaders(200,response.length);
      try(var body=exchange.getResponseBody()){body.write(response);}
    });
    server.start();
    ReflectionTestUtils.setField(aiModel,"baseUrl","http://127.0.0.1:"+server.getAddress().getPort()+"/v1");
    ReflectionTestUtils.setField(aiModel,"apiKey","test-key");
    ReflectionTestUtils.setField(aiModel,"model","mock-model");
    try{
      aiProcessor.process(task);
      assertThat(jdbc.queryForObject("select status from ai_analysis_tasks where id=?",String.class,task)).isEqualTo("SUCCEEDED");
      assertThat(jdbc.queryForObject("select risk_level from emotion_records where id=?",String.class,record)).isEqualTo("CRITICAL");
      assertThat(jdbc.queryForObject("select moderation_status from emotion_records where id=?",String.class,record)).isEqualTo("PENDING_REVIEW");
      assertThat(jdbc.queryForObject("select recommendation_eligible from emotion_records where id=?",Boolean.class,record)).isFalse();
      assertThat(jdbc.queryForObject("select count(*) from risk_cases where record_id=?",Long.class,record)).isEqualTo(1L);
      assertThat(jdbc.queryForObject("select count(*) from ai_analysis_results where task_id=? and source='MODEL'",Long.class,task)).isEqualTo(1L);
    }finally{clearModelConfiguration();server.stop(0);}
  }

  @Test @Transactional void threeModelFailuresLeaveTheLocalRiskUnchanged(){
    var student=java.util.UUID.randomUUID();var record=java.util.UUID.randomUUID();var task=java.util.UUID.randomUUID();
    insertStudentAndRecord(student,record,"PUBLIC");
    jdbc.update("insert into ai_analysis_tasks(id,record_id,status) values (?,?,?)",task,record,"PENDING");
    for(int i=0;i<3;i++)aiProcessor.markFailure(task,new IllegalArgumentException("Do not store raw model response"));
    assertThat(jdbc.queryForObject("select status from ai_analysis_tasks where id=?",String.class,task)).isEqualTo("FAILED");
    assertThat(jdbc.queryForObject("select attempts from ai_analysis_tasks where id=?",Integer.class,task)).isEqualTo(3);
    assertThat(jdbc.queryForObject("select last_error from ai_analysis_tasks where id=?",String.class,task)).isEqualTo("Model response was invalid");
    assertThat(jdbc.queryForObject("select risk_level from emotion_records where id=?",String.class,record)).isEqualTo("LOW");
  }

  @Test void malformedExternalResponseRetriesWithoutPartialModelResult() throws Exception {
    var student=java.util.UUID.randomUUID();var record=java.util.UUID.randomUUID();var task=java.util.UUID.randomUUID();
    insertStudentAndRecord(student,record,"PUBLIC");
    jdbc.update("insert into ai_analysis_tasks(id,record_id,status) values (?,?,?)",task,record,"PENDING");
    var server=HttpServer.create(new java.net.InetSocketAddress("127.0.0.1",0),0);
    server.createContext("/v1/chat/completions",exchange->{
      byte[] response="{\"choices\":[{\"message\":{\"content\":\"{\\\"riskLevel\\\":\\\"LOW\\\"}\"}}]}".getBytes(java.nio.charset.StandardCharsets.UTF_8);
      exchange.getResponseHeaders().set("Content-Type","application/json");exchange.sendResponseHeaders(200,response.length);
      try(var body=exchange.getResponseBody()){body.write(response);}
    });
    server.start();
    ReflectionTestUtils.setField(aiModel,"baseUrl","http://127.0.0.1:"+server.getAddress().getPort()+"/v1");
    ReflectionTestUtils.setField(aiModel,"apiKey","test-key");
    ReflectionTestUtils.setField(aiModel,"model","mock-model");
    try{
      for(int i=0;i<3;i++){
        try{aiProcessor.process(task);org.junit.jupiter.api.Assertions.fail("Invalid model response must be rejected");}
        catch(IllegalArgumentException expected){aiProcessor.markFailure(task,expected);}
      }
      assertThat(jdbc.queryForObject("select status from ai_analysis_tasks where id=?",String.class,task)).isEqualTo("FAILED");
      assertThat(jdbc.queryForObject("select attempts from ai_analysis_tasks where id=?",Integer.class,task)).isEqualTo(3);
      assertThat(jdbc.queryForObject("select risk_level from emotion_records where id=?",String.class,record)).isEqualTo("LOW");
      assertThat(jdbc.queryForObject("select count(*) from ai_analysis_results where task_id=? and source='MODEL'",Long.class,task)).isZero();
    }finally{clearModelConfiguration();server.stop(0);}
  }

  @Test void unavailableModelLeavesLocalDecisionActive(){
    var student=java.util.UUID.randomUUID();var record=java.util.UUID.randomUUID();var task=java.util.UUID.randomUUID();
    insertStudentAndRecord(student,record,"PUBLIC");
    jdbc.update("insert into ai_analysis_tasks(id,record_id,status) values (?,?,?)",task,record,"PENDING");
    ReflectionTestUtils.setField(aiModel,"baseUrl","http://127.0.0.1:1/v1");
    ReflectionTestUtils.setField(aiModel,"apiKey","test-key");
    ReflectionTestUtils.setField(aiModel,"model","mock-model");
    try{
      Exception failure=null;
      try{aiProcessor.process(task);}catch(Exception e){failure=e;}
      assertThat(failure).isNotNull();
      aiProcessor.markFailure(task,failure);
      assertThat(jdbc.queryForObject("select status from ai_analysis_tasks where id=?",String.class,task)).isEqualTo("PENDING");
      assertThat(jdbc.queryForObject("select attempts from ai_analysis_tasks where id=?",Integer.class,task)).isEqualTo(1);
      assertThat(jdbc.queryForObject("select risk_level from emotion_records where id=?",String.class,record)).isEqualTo("LOW");
      assertThat(jdbc.queryForObject("select count(*) from ai_analysis_results where task_id=? and source='MODEL'",Long.class,task)).isZero();
    }finally{clearModelConfiguration();}
  }

  @Test @Transactional void lowModelRatingCannotDowngradeExistingHighRisk() throws Exception {
    var student=java.util.UUID.randomUUID();var record=java.util.UUID.randomUUID();var task=java.util.UUID.randomUUID();
    insertStudentAndRecord(student,record,"PUBLIC");
    jdbc.update("update emotion_records set risk_level='HIGH',recommendation_eligible=false,moderation_status='PENDING_REVIEW' where id=?",record);
    jdbc.update("insert into ai_analysis_tasks(id,record_id,status) values (?,?,?)",task,record,"PENDING");
    var server=HttpServer.create(new java.net.InetSocketAddress("127.0.0.1",0),0);
    server.createContext("/v1/chat/completions",exchange->{
      String modelResult="{\"riskLevel\":\"LOW\",\"primaryEmotion\":\"Sad\",\"intensity\":4,\"explicitPlan\":false,\"imminentTiming\":false,\"meansPresent\":false,\"violationCategory\":\"NONE\",\"confidence\":0.9,\"supportCard\":\"ENCOURAGEMENT\",\"reason\":\"No additional signal\"}";
      byte[] response=tools.jackson.databind.json.JsonMapper.builder().build().writeValueAsBytes(java.util.Map.of("choices",new Object[]{java.util.Map.of("message",java.util.Map.of("content",modelResult))}));
      exchange.getResponseHeaders().set("Content-Type","application/json");exchange.sendResponseHeaders(200,response.length);
      try(var body=exchange.getResponseBody()){body.write(response);}
    });
    server.start();
    ReflectionTestUtils.setField(aiModel,"baseUrl","http://127.0.0.1:"+server.getAddress().getPort()+"/v1");
    ReflectionTestUtils.setField(aiModel,"apiKey","test-key");
    ReflectionTestUtils.setField(aiModel,"model","mock-model");
    try{
      aiProcessor.process(task);
      assertThat(jdbc.queryForObject("select risk_level from emotion_records where id=?",String.class,record)).isEqualTo("HIGH");
      assertThat(jdbc.queryForObject("select recommendation_eligible from emotion_records where id=?",Boolean.class,record)).isFalse();
      assertThat(jdbc.queryForObject("select count(*) from risk_cases where record_id=?",Long.class,record)).isEqualTo(1L);
    }finally{clearModelConfiguration();server.stop(0);}
  }

  private void insertStudentAndRecord(java.util.UUID student,java.util.UUID record,String visibility){
    jdbc.update("insert into users(id,email,password_hash,display_name,role,status) values (?,?,?,?,?,?)",student,"ai-student-"+student+"@example.test","test","Student","STUDENT","ACTIVE");
    jdbc.update("insert into emotion_records(id,author_id,title,content,visibility,emotion,intensity,anonymous_alias,risk_level) values (?,?,?,?,?,?,?,?,?)",record,student,"Test","A test entry for classification",visibility,"Sad",5,"Stranger","LOW");
  }
  private void clearModelConfiguration(){ReflectionTestUtils.setField(aiModel,"baseUrl","");ReflectionTestUtils.setField(aiModel,"apiKey","");ReflectionTestUtils.setField(aiModel,"model","");}
}
