package cn.campus.treehole.repo;
import cn.campus.treehole.domain.CaseMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface CaseMessageRepository extends JpaRepository<CaseMessage, UUID> {
  List<CaseMessage> findByRiskCaseIdOrderByCreatedAtAsc(UUID caseId);
}

