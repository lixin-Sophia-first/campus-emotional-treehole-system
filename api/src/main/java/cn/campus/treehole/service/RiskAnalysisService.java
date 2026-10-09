package cn.campus.treehole.service;

import cn.campus.treehole.domain.Enums.RiskLevel;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;

@Service @RequiredArgsConstructor
public class RiskAnalysisService {
  private final RiskRuleCatalog ruleCatalog;
  public record Analysis(RiskLevel level,List<String> reasons,String card,boolean violation,
                         String violationCategory,boolean explicitPlan,boolean imminentTiming,
                         boolean meansPresent,double confidence,String primaryEmotion,int intensity) {}

  private static final List<String> DIRECT_SELF_HARM=List.of(
    "kill myself","end my life","take my life","take my own life","cut myself",
    "hurt myself","harm myself","i want to die","i'm suicidal","i am suicidal");
  private static final List<Pattern> CONTEXTUAL_SELF_HARM=List.of(
    Pattern.compile("\\bi(?:'m| am| have been)?\\s+(?:thinking about|considering|planning|going to|want to)\\s+(?:committing |attempting )?suicide\\b"),
    Pattern.compile("\\bi(?:'m| am)?\\s+(?:thinking about|considering|planning|going to|want to)\\s+(?:an? )?overdose\\b"),
    Pattern.compile("\\bi(?:'m| am)?\\s+(?:thinking about|considering|planning|going to|want to)\\s+jump off\\s+(?:a |the )?(?:bridge|roof)\\b")
  );
  private static final List<String> HIGH=List.of("do not want to live","don't want to live",
    "life is meaningless","want to disappear","cannot go on","can't go on","no hope","better off without me");
  private static final List<String> MEDIUM=List.of("very anxious","extremely anxious","cannot sleep",
    "can't sleep","so lonely","feel hopeless","sad for weeks","nobody understands","panic attack","feel overwhelmed");
  private static final List<String> LOW=List.of("annoyed","stressed","sad","upset","tired","worried");
  private static final List<String> PLAN=List.of("i have a plan to","i planned to","i am going to kill",
    "i'm going to kill","i am going to end my life","i'm going to end my life",
    "i plan to kill myself","i will kill","i will end my life","i decided to end my life");
  private static final List<String> TIMING=List.of("right now","tonight","today","this morning",
    "this evening","in an hour","soon");
  private static final List<String> MEANS=List.of("pills","knife","blade","rope","gun","bridge","roof","poison");
  private static final Map<String,List<String>> VIOLATIONS=Map.of(
    "HARASSMENT",List.of("go die","worthless idiot","i will expose you","doxxing"),
    "SEXUAL_CONTENT",List.of("sexual services","explicit photos","pornographic"),
    "ILLEGAL_CONTENT",List.of("fake documents","exam cheating","sell drugs"),
    "SPAM",List.of("contact me to buy","guaranteed profit","click my link")
  );

  public Analysis analyze(String text){
    String normalized=text.toLowerCase(Locale.ROOT).replace('’','\'').replaceAll("\\s+"," ").trim();
    var reasons=new ArrayList<String>();
    boolean self=firstAffirmed(normalized,DIRECT_SELF_HARM,reasons,"Self-harm expression")
      || contextualSelfHarm(normalized,reasons);
    boolean plan=firstAffirmed(normalized,PLAN,reasons,"Explicit plan");
    boolean timing=firstAffirmed(normalized,TIMING,reasons,"Imminent timing");
    boolean means=firstAffirmed(normalized,MEANS,reasons,"Potential means");
    RiskLevel level=RiskLevel.LOW;
    if(self&&(plan||timing||means))level=RiskLevel.CRITICAL;
    else if(self||firstAffirmed(normalized,HIGH,reasons,"Severe hopelessness")
      ||firstAffirmed(normalized,ruleCatalog.activeHighPhrases(),reasons,"Reviewed risk rule"))level=RiskLevel.HIGH;
    else if(firstAffirmed(normalized,MEDIUM,reasons,"Sustained distress"))level=RiskLevel.MEDIUM;
    else firstAffirmed(normalized,LOW,reasons,"Everyday distress");

    String violation="NONE";
    for(var entry:VIOLATIONS.entrySet())if(entry.getValue().stream().anyMatch(normalized::contains)){
      violation=entry.getKey();reasons.add("Content moderation match: "+violation);break;
    }
    String emotion=normalized.contains("anxious")||normalized.contains("panic")?"Anxious"
      :normalized.contains("angry")?"Angry":normalized.contains("lonely")?"Lonely"
      :normalized.contains("sad")||normalized.contains("hopeless")?"Sad":"Unspecified";
    int intensity=switch(level){case LOW->4;case MEDIUM->6;case HIGH->8;case CRITICAL->10;};
    double confidence=reasons.isEmpty()?.35:Math.min(.99,.62+reasons.size()*.09);
    String card=switch(level){case LOW->"ENCOURAGEMENT";case MEDIUM->"GROUNDING";
      case HIGH->"SEEK_SUPPORT";case CRITICAL->"URGENT_SUPPORT";};
    return new Analysis(level,List.copyOf(reasons),card,!"NONE".equals(violation),violation,
      plan,timing,means,confidence,emotion,intensity);
  }

  private boolean contextualSelfHarm(String text,List<String> reasons){
    for(var pattern:CONTEXTUAL_SELF_HARM)if(pattern.matcher(text).find()){
      reasons.add("First-person self-harm intent");return true;
    }
    return false;
  }
  public boolean matchesReviewedPhrase(String text,String phrase){
    String normalized=text.toLowerCase(Locale.ROOT).replace('’','\'').replaceAll("\\s+"," ").trim();
    return firstAffirmed(normalized,List.of(phrase.toLowerCase(Locale.ROOT)),new ArrayList<>(),"Reviewed risk rule");
  }
  private boolean firstAffirmed(String text,List<String> phrases,List<String> reasons,String label){
    for(String phrase:phrases){
      int from=0,at;
      while((at=text.indexOf(phrase,from))>=0){
        if(!negated(text,at)){reasons.add(label);return true;}
        from=at+phrase.length();
      }
    }
    return false;
  }
  private boolean negated(String text,int at){
    String before=text.substring(Math.max(0,at-32),at);
    return before.matches(".*\\b(not|never|no longer|don't|do not|won't|wouldn't)\\b.{0,20}$");
  }
}
