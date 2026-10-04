package cn.campus.treehole.service;

import cn.campus.treehole.domain.Enums.RiskLevel;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class RiskAnalysisService {
  public record Analysis(RiskLevel level,List<String> reasons,String card,boolean violation,String violationCategory,boolean explicitPlan,boolean imminentTiming,boolean meansPresent,double confidence,String primaryEmotion,int intensity) {}
  private static final List<String> SELF_HARM=List.of("kill myself","end my life","take my life","suicide","cut myself","hurt myself","jump off","overdose");
  private static final List<String> HIGH=List.of("do not want to live","don't want to live","life is meaningless","want to disappear","cannot go on","can't go on","no hope","better off without me");
  private static final List<String> MEDIUM=List.of("very anxious","cannot sleep","can't sleep","so lonely","feel hopeless","sad for weeks","nobody understands","panic attack");
  private static final List<String> LOW=List.of("annoyed","stressed","sad","upset","tired","worried");
  private static final List<String> PLAN=List.of("i have a plan","i planned","i am going to","i decided to","i will kill","i will end");
  private static final List<String> TIMING=List.of("right now","tonight","today","this morning","this evening","in an hour","soon");
  private static final List<String> MEANS=List.of("pills","knife","blade","rope","gun","bridge","roof","poison");
  private static final Map<String,List<String>> VIOLATIONS=Map.of(
    "HARASSMENT",List.of("go die","worthless idiot","i will expose you","doxxing"),
    "SEXUAL_CONTENT",List.of("sexual services","explicit photos","pornographic"),
    "ILLEGAL_CONTENT",List.of("fake documents","exam cheating","sell drugs"),
    "SPAM",List.of("contact me to buy","guaranteed profit","click my link")
  );
  public Analysis analyze(String text){
    String n=text.toLowerCase(Locale.ROOT).replaceAll("\\s+"," ").trim();var reasons=new ArrayList<String>();boolean self=firstAffirmed(n,SELF_HARM,reasons,"Self-harm expression");boolean plan=firstAffirmed(n,PLAN,reasons,"Explicit plan");boolean timing=firstAffirmed(n,TIMING,reasons,"Imminent timing");boolean means=firstAffirmed(n,MEANS,reasons,"Potential means");RiskLevel level=RiskLevel.LOW;
    if(self&&(plan||timing||means))level=RiskLevel.CRITICAL;else if(self||firstAffirmed(n,HIGH,reasons,"Severe hopelessness"))level=RiskLevel.HIGH;else if(firstAffirmed(n,MEDIUM,reasons,"Sustained distress"))level=RiskLevel.MEDIUM;else firstAffirmed(n,LOW,reasons,"Everyday distress");
    String violation="NONE";for(var e:VIOLATIONS.entrySet())if(e.getValue().stream().anyMatch(n::contains)){violation=e.getKey();reasons.add("Content moderation match: "+e.getKey());break;}
    String emotion=n.contains("anxious")||n.contains("panic")?"Anxious":n.contains("angry")?"Angry":n.contains("lonely")?"Lonely":n.contains("sad")||n.contains("hopeless")?"Sad":"Unspecified";int intensity=switch(level){case LOW->4;case MEDIUM->6;case HIGH->8;case CRITICAL->10;};double confidence=reasons.isEmpty()?.35:Math.min(.99,.62+reasons.size()*.09);String card=switch(level){case LOW->"ENCOURAGEMENT";case MEDIUM->"GROUNDING";case HIGH->"SEEK_SUPPORT";case CRITICAL->"URGENT_SUPPORT";};return new Analysis(level,List.copyOf(reasons),card,!"NONE".equals(violation),violation,plan,timing,means,confidence,emotion,intensity);
  }
  private boolean firstAffirmed(String text,List<String> phrases,List<String> reasons,String label){for(String phrase:phrases){int at=text.indexOf(phrase);if(at>=0&&!negated(text,at)){reasons.add(label+": "+phrase);return true;}}return false;}
  private boolean negated(String text,int at){String before=text.substring(Math.max(0,at-32),at);return before.matches(".*\\b(not|never|no longer|don't|do not|won't|wouldn't)\\b.{0,20}$");}
}
