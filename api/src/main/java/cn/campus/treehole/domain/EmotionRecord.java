package cn.campus.treehole.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;
import static cn.campus.treehole.domain.Enums.*;

@Entity @Table(name="emotion_records") @Getter @Setter @NoArgsConstructor
public class EmotionRecord {
  @Id @GeneratedValue(strategy=GenerationType.UUID) private UUID id;
  @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="author_id") private User author;
  @Column(nullable=false, columnDefinition="text") private String content;
  @Enumerated(EnumType.STRING) @Column(nullable=false) private Visibility visibility;
  @Column(nullable=false) private String emotion;
  @Column(nullable=false) private int intensity;
  @Column(nullable=false) private String tags = "";
  @Column(name="anonymous_alias", nullable=false) private String anonymousAlias;
  @Enumerated(EnumType.STRING) @Column(name="risk_level", nullable=false) private RiskLevel riskLevel = RiskLevel.LOW;
  @Column(name="risk_reasons", nullable=false, columnDefinition="text") private String riskReasons = "";
  @Column(name="support_card", nullable=false) private String supportCard = "ENCOURAGEMENT";
  @Column(name="recommendation_eligible", nullable=false) private boolean recommendationEligible = true;
  @Enumerated(EnumType.STRING) @Column(name="moderation_status", nullable=false) private ModerationStatus moderationStatus = ModerationStatus.PUBLISHED;
  @Column(name="violation_category",nullable=false) private String violationCategory="NONE";
  @Column(name="deleted_at") private Instant deletedAt;
  @Column(name="created_at", insertable=false, updatable=false) private Instant createdAt;
  @Column(name="updated_at", insertable=false, updatable=false) private Instant updatedAt;
}
