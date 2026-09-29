package cn.campus.treehole.repo;
import cn.campus.treehole.domain.RiskCase;
import cn.campus.treehole.domain.Enums.CaseStatus;
import org.springframework.data.jpa.repository.*;
import jakarta.persistence.LockModeType;
import java.util.*;
public interface RiskCaseRepository extends JpaRepository<RiskCase, UUID> {
  List<RiskCase> findAllByOrderByCreatedAtDesc();
  List<RiskCase> findByStudentIdOrderByCreatedAtDesc(UUID studentId);
  Optional<RiskCase> findByRecordId(UUID recordId);
  @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select c from RiskCase c where c.id=:id") Optional<RiskCase> findLockedById(UUID id);
  long countByStatusNot(CaseStatus status);
}
