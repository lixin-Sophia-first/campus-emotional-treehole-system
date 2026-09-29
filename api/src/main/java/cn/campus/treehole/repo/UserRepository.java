package cn.campus.treehole.repo;
import cn.campus.treehole.domain.User;
import cn.campus.treehole.domain.Enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface UserRepository extends JpaRepository<User, UUID> {
  Optional<User> findByEmailIgnoreCase(String email);
  boolean existsByRole(Role role);
}

