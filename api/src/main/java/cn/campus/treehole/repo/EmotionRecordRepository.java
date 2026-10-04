package cn.campus.treehole.repo;
import cn.campus.treehole.domain.*;
import cn.campus.treehole.domain.Enums.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import java.util.*;
public interface EmotionRecordRepository extends JpaRepository<EmotionRecord, UUID> {
  @Query("select r from EmotionRecord r where r.visibility='PUBLIC' and r.moderationStatus='PUBLISHED' and r.deletedAt is null and r.recommendationEligible=true order by r.createdAt desc")
  Page<EmotionRecord> recommended(Pageable pageable);
  @Query("select r from EmotionRecord r where r.visibility='PUBLIC' and r.moderationStatus='PUBLISHED' and r.deletedAt is null order by r.createdAt desc")
  Page<EmotionRecord> latest(Pageable pageable);
  @Query(value="select e.* from emotion_records e where e.visibility='PUBLIC' and e.moderation_status='PUBLISHED' and e.deleted_at is null and e.recommendation_eligible=true order by ((select count(*) from likes l where l.record_id=e.id)*2+(select count(*) from comments c where c.record_id=e.id and c.deleted_at is null)) desc,e.created_at desc",countQuery="select count(*) from emotion_records e where e.visibility='PUBLIC' and e.moderation_status='PUBLISHED' and e.deleted_at is null and e.recommendation_eligible=true",nativeQuery=true)
  Page<EmotionRecord> hot(Pageable pageable);
  @Query("select r from EmotionRecord r where r.visibility='PUBLIC' and r.moderationStatus='PUBLISHED' and r.deletedAt is null and (:query='' or lower(r.content) like lower(concat('%',:query,'%'))) and (:tag='' or lower(r.tags) like lower(concat('%',:tag,'%'))) order by r.createdAt desc")
  Page<EmotionRecord> search(String query,String tag,Pageable pageable);
  Page<EmotionRecord> findByAuthorIdAndDeletedAtIsNullOrderByCreatedAtDesc(UUID authorId, Pageable pageable);
  Optional<EmotionRecord> findByIdAndDeletedAtIsNull(UUID id);
}
