package cn.campus.treehole.service;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static cn.campus.treehole.domain.Enums.RiskLevel;

class RiskAnalysisServiceTest {
  private final RiskAnalysisService service=new RiskAnalysisService();
  @Test void keepsEverydayStressLow(){ assertThat(service.analyze("最近考试压力有点大").level()).isEqualTo(RiskLevel.LOW); }
  @Test void detectsSustainedDistress(){ assertThat(service.analyze("我最近很焦虑，也总是睡不着").level()).isEqualTo(RiskLevel.MEDIUM); }
  @Test void detectsHighRiskLanguage(){ assertThat(service.analyze("真的撑不下去了，活着没意义").level()).isEqualTo(RiskLevel.HIGH); }
  @Test void detectsImmediateCriticalRisk(){ assertThat(service.analyze("我已经准备好，今晚自杀").level()).isEqualTo(RiskLevel.CRITICAL); }
  @Test void moderationIsSeparateFromEmotionRisk(){ var a=service.analyze("加微信代考"); assertThat(a.violation()).isTrue(); assertThat(a.level()).isEqualTo(RiskLevel.LOW); }
}

