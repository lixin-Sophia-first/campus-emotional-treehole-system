package cn.campus.treehole.service;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static cn.campus.treehole.domain.Enums.RiskLevel;

class RiskAnalysisServiceTest {
  private final RiskAnalysisService service=new RiskAnalysisService();
  @Test void keepsEverydayStressLow(){ assertThat(service.analyze("I feel stressed about exams").level()).isEqualTo(RiskLevel.LOW); }
  @Test void detectsSustainedDistress(){ assertThat(service.analyze("I am very anxious and cannot sleep").level()).isEqualTo(RiskLevel.MEDIUM); }
  @Test void detectsHighRiskLanguage(){ assertThat(service.analyze("I cannot go on and there is no hope").level()).isEqualTo(RiskLevel.HIGH); }
  @Test void detectsImmediateCriticalRisk(){ assertThat(service.analyze("I have a plan to kill myself now").level()).isEqualTo(RiskLevel.CRITICAL); }
  @Test void understandsNegatedSelfHarmLanguage(){ assertThat(service.analyze("I do not want to kill myself").level()).isEqualTo(RiskLevel.LOW); }
  @Test void explicitMeansMakeSelfHarmCritical(){ var a=service.analyze("I want to end my life with pills tonight");assertThat(a.level()).isEqualTo(RiskLevel.CRITICAL);assertThat(a.meansPresent()).isTrue();assertThat(a.imminentTiming()).isTrue(); }
  @Test void moderationIsSeparateFromEmotionRisk(){ var a=service.analyze("Contact me for exam cheating"); assertThat(a.violation()).isTrue(); assertThat(a.level()).isEqualTo(RiskLevel.LOW); }
}
