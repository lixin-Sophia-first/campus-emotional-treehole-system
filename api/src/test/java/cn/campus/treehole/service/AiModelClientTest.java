package cn.campus.treehole.service;

import cn.campus.treehole.domain.Enums.RiskLevel;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.json.JsonMapper;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.*;

class AiModelClientTest {
  private final JsonMapper json=JsonMapper.builder().build();
  private final AiModelClient client=new AiModelClient(json);
  private final String valid="""
    {"riskLevel":"LOW","primaryEmotion":"Sad","intensity":5,"explicitPlan":true,
     "imminentTiming":true,"meansPresent":false,"violationCategory":"NONE",
     "confidence":0.91,"supportCard":"ENCOURAGEMENT","reason":"Strong safety signal"}
    """;

  @Test void rejectsIncompleteOrMalformedModelResults(){
    assertThatThrownBy(()->client.parse("{\"riskLevel\":\"LOW\"}"))
      .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("primaryEmotion");
    assertThatThrownBy(()->client.parse(valid.replace("\"confidence\":0.91","\"confidence\":1.5")))
      .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("confidence");
    assertThatThrownBy(()->client.parse("not JSON"))
      .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("malformed");
  }

  @Test void strongSignalsCannotBeDowngradedByModelRiskLabel(){
    assertThat(client.parse(valid).riskLevel()).isEqualTo(RiskLevel.CRITICAL);
  }

  @Test void callsOpenAiCompatibleEndpointAndParsesStructuredResponse() throws Exception {
    var received=new AtomicReference<String>();
    var server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
    server.createContext("/v1/chat/completions",exchange->{
      received.set(new String(exchange.getRequestBody().readAllBytes(),StandardCharsets.UTF_8));
      byte[] response=json.writeValueAsBytes(Map.of("choices",new Object[]{Map.of("message",Map.of("content",valid))}));
      exchange.getResponseHeaders().set("Content-Type","application/json");
      exchange.sendResponseHeaders(200,response.length);
      try(var body=exchange.getResponseBody()){body.write(response);}
    });
    server.start();
    try{
      ReflectionTestUtils.setField(client,"baseUrl","http://127.0.0.1:"+server.getAddress().getPort()+"/v1");
      ReflectionTestUtils.setField(client,"apiKey","test-key");
      ReflectionTestUtils.setField(client,"model","mock-model");
      var result=client.analyzePublicText("A public test post");
      assertThat(result.riskLevel()).isEqualTo(RiskLevel.CRITICAL);
      assertThat(received.get()).contains("A public test post").contains("mock-model");
    }finally{server.stop(0);}
  }
}
