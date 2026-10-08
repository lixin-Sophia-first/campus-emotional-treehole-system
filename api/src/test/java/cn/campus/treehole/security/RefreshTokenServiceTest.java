package cn.campus.treehole.security;

import cn.campus.treehole.repo.UserRepository;
import cn.campus.treehole.web.ApiErrorHandler.ApiException;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class RefreshTokenServiceTest {
  @Test void replayOfRotatedTokenRevokesRemainingSessions(){
    JdbcTemplate jdbc=mock(JdbcTemplate.class);
    UserRepository users=mock(UserRepository.class);
    UUID userId=UUID.randomUUID();
    when(jdbc.queryForList(anyString(),anyString())).thenReturn(List.of(Map.of(
      "id",UUID.randomUUID(),"user_id",userId,"revoked_at",Instant.now(),
      "replaced_by",UUID.randomUUID(),"unexpired",true
    )));

    assertThatThrownBy(()->new RefreshTokenService(jdbc,users).rotate("already-used-token"))
      .isInstanceOf(ApiException.class)
      .hasMessageContaining("already used");

    verify(jdbc).update(contains("where user_id=? and revoked_at is null"),eq(userId));
    verifyNoInteractions(users);
  }
}
