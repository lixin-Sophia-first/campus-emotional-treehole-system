package cn.campus.treehole.service;

import cn.campus.treehole.domain.Enums.RiskLevel;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class RiskAnalysisService {
  public record Analysis(RiskLevel level,List<String> reasons,String card,boolean violation) {}
  private static final Map<RiskLevel,List<String>> WORDS=Map.of(
    RiskLevel.CRITICAL,List.of("现在就去死","今晚自杀","已经准备好","不想活了马上","结束生命","割腕","跳楼"),
    RiskLevel.HIGH,List.of("不想活了","活着没意义","想消失","自残","撑不下去","没有希望"),
    RiskLevel.MEDIUM,List.of("很焦虑","睡不着","好孤独","很绝望","持续难过","没人理解"),
    RiskLevel.LOW,List.of("烦","压力","难过","郁闷","疲惫")
  );
  private static final List<String> VIOLATIONS=List.of("加微信代考","办证","博彩","色情交易","去死吧","人肉搜索");
  public Analysis analyze(String text){
    var reasons=new ArrayList<String>(); var level=RiskLevel.LOW;
    for(var candidate:List.of(RiskLevel.CRITICAL,RiskLevel.HIGH,RiskLevel.MEDIUM,RiskLevel.LOW)) {
      for(String word:WORDS.get(candidate)) if(text.contains(word)){ reasons.add("命中风险表达："+word); level=candidate; break; }
      if(!reasons.isEmpty()) break;
    }
    boolean violation=VIOLATIONS.stream().anyMatch(text::contains);
    if(violation) reasons.add("命中内容审核规则");
    String card=switch(level){case LOW->"ENCOURAGEMENT"; case MEDIUM->"GROUNDING"; case HIGH->"SEEK_SUPPORT"; case CRITICAL->"URGENT_SUPPORT";};
    return new Analysis(level,reasons,card,violation);
  }
}

