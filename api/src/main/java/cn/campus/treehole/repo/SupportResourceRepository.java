package cn.campus.treehole.repo;
import cn.campus.treehole.domain.SupportResource;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface SupportResourceRepository extends JpaRepository<SupportResource, UUID> {
  List<SupportResource> findByActiveTrueOrderByUpdatedAtDesc();
}

