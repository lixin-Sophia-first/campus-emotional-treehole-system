package cn.campus.treehole.repo;
import cn.campus.treehole.domain.TeacherApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface TeacherApplicationRepository extends JpaRepository<TeacherApplication, UUID> {
  boolean existsByEmailIgnoreCase(String email);
  List<TeacherApplication> findAllByOrderByCreatedAtDesc();
}

