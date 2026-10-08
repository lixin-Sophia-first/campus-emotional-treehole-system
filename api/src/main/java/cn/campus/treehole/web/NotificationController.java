package cn.campus.treehole.web;
import cn.campus.treehole.service.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import java.util.*;
@RestController @RequestMapping("/api/v1/notifications") @RequiredArgsConstructor
public class NotificationController {
  private final JdbcTemplate jdbc; private final CurrentUserService current;
  @GetMapping public List<Map<String,Object>> list(){ return jdbc.queryForList("select id,title,content,target_path,read_at,created_at from notifications where user_id=? order by created_at desc limit 100",current.get().getId()); }
  @PostMapping("/{id}/read") public void read(@PathVariable UUID id){ jdbc.update("update notifications set read_at=now() where id=? and user_id=?",id,current.get().getId()); }
}
