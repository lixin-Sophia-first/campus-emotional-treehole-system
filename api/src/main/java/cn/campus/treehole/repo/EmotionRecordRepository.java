package cn.campus.treehole.repo;
import cn.campus.treehole.domain.*;
import cn.campus.treehole.domain.Enums.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import java.util.*;
public interface EmotionRecordRepository extends JpaRepository<EmotionRecord, UUID> {
  @Query("select r from EmotionRecord r where r.visibility='PUBLIC' and r.moderationStatus='PUBLISHED' and r.deletedAt is null and (:recommended=false or r.recommendationEligible=true) order by r.createdAt desc")
  Page<EmotionRecord> feed(boolean recommended, Pageable pageable);
  Page<EmotionRecord> findByAuthorIdAndDeletedAtIsNullOrderByCreatedAtDesc(UUID authorId, Pageable pageable);
  Optional<EmotionRecord> findByIdAndDeletedAtIsNull(UUID id);
}

