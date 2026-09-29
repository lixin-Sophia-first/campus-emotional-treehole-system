package cn.campus.treehole.repo;
import cn.campus.treehole.domain.IdentityAccessRequest;
import cn.campus.treehole.domain.Enums.AccessStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface IdentityAccessRequestRepository extends JpaRepository<IdentityAccessRequest, UUID> {
  boolean existsByRiskCaseIdAndTeacherIdAndStatus(UUID caseId,UUID teacherId,AccessStatus status);
  List<IdentityAccessRequest> findAllByOrderByCreatedAtDesc();
}

