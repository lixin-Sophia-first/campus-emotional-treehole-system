package cn.campus.treehole;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest(properties={"spring.mail.host=localhost","spring.mail.port=1025"})
class PostgresContextTest {
  @Container @ServiceConnection
  static PostgreSQLContainer<?> postgres=new PostgreSQLContainer<>("postgres:17-alpine");

  @Test void applicationStartsWithRealPostgres(){assertThat(postgres.isRunning()).isTrue();}
}
