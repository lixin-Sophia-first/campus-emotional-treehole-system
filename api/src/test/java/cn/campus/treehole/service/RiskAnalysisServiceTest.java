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
  @Test void preventionArticleIsNotAnEmergency(){assertThat(service.analyze("I read a suicide prevention article today").level()).isEqualTo(RiskLevel.LOW);}
  @Test void ordinaryJumpingPhraseIsNotSelfHarm(){assertThat(service.analyze("I am going to jump off the bus today").level()).isEqualTo(RiskLevel.LOW);}
  @Test void firstPersonOverdoseIntentIsCriticalWhenImminent(){assertThat(service.analyze("I want to overdose tonight").level()).isEqualTo(RiskLevel.CRITICAL);}
  @Test void firstPersonWishToDieIsHigh(){assertThat(service.analyze("I want to die").level()).isEqualTo(RiskLevel.HIGH);}
  @Test void laterAffirmedPhraseIsNotLostAfterAnEarlierNegation(){assertThat(service.analyze("I do not want to kill myself. Later, I will kill myself tonight.").level()).isEqualTo(RiskLevel.CRITICAL);}
  @Test void ordinaryFutureTenseDoesNotCountAsAnExplicitPlan(){var a=service.analyze("I am going to class, but I want to kill myself");assertThat(a.level()).isEqualTo(RiskLevel.HIGH);assertThat(a.explicitPlan()).isFalse();}
  @Test void explicitlyPlannedSelfHarmIsCriticalWithoutASeparateTimeWord(){assertThat(service.analyze("I'm going to end my life").level()).isEqualTo(RiskLevel.CRITICAL);}
  @Test void savedRuleReasonsDescribeCategoriesWithoutRepeatingPrivatePhrases(){var a=service.analyze("I have a plan to kill myself with pills tonight");assertThat(a.reasons()).contains("Self-harm expression","Explicit plan","Potential means");assertThat(String.join("; ",a.reasons())).doesNotContain("kill myself","pills","tonight");}
}
