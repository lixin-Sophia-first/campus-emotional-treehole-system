package cn.campus.treehole.service;

import cn.campus.treehole.domain.Enums.RiskLevel;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class RiskAnalysisService {
  public record Analysis(RiskLevel level,List<String> reasons,String card,boolean violation) {}
  private static final Map<RiskLevel,List<String>> WORDS=Map.of(
    RiskLevel.CRITICAL,List.of("kill myself now","suicide tonight","I have a plan","end my life","cut my wrists","jump off"),
    RiskLevel.HIGH,List.of("do not want to live","life is meaningless","want to disappear","self-harm","cannot go on","no hope"),
    RiskLevel.MEDIUM,List.of("very anxious","cannot sleep","so lonely","feel hopeless","sad for weeks","nobody understands"),
    RiskLevel.LOW,List.of("annoyed","stressed","sad","upset","tired")
  );
  private static final List<String> VIOLATIONS=List.of("contact me for exam cheating","fake documents","gambling","sexual services","go die","doxxing");
  public Analysis analyze(String text){
    String normalized=text.toLowerCase(Locale.ROOT); var reasons=new ArrayList<String>(); var level=RiskLevel.LOW;
    for(var candidate:List.of(RiskLevel.CRITICAL,RiskLevel.HIGH,RiskLevel.MEDIUM,RiskLevel.LOW)) {
      for(String word:WORDS.get(candidate)) if(normalized.contains(word)){ reasons.add("Matched risk expression: "+word); level=candidate; break; }
      if(!reasons.isEmpty()) break;
    }
    boolean violation=VIOLATIONS.stream().anyMatch(normalized::contains);
    if(violation) reasons.add("Matched a content-moderation rule");
    String card=switch(level){case LOW->"ENCOURAGEMENT"; case MEDIUM->"GROUNDING"; case HIGH->"SEEK_SUPPORT"; case CRITICAL->"URGENT_SUPPORT";};
    return new Analysis(level,reasons,card,violation);
  }
}
