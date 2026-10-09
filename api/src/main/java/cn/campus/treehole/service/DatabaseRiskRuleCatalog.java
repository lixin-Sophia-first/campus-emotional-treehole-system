package cn.campus.treehole.service;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service @RequiredArgsConstructor
public class DatabaseRiskRuleCatalog implements RiskRuleCatalog {
  private final JdbcTemplate jdbc;

  @Override public List<String> activeHighPhrases(){
    return jdbc.queryForList("""
      select c.phrase from risk_rule_candidates c
      join emotion_records e on e.id=c.record_id
      where c.status='ACTIVE' and e.visibility='PUBLIC' and e.deleted_at is null
      order by c.version
      """,String.class);
  }
}
