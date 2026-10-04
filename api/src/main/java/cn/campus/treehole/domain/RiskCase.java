package cn.campus.treehole.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;
import static cn.campus.treehole.domain.Enums.*;

@Entity @Table(name="risk_cases") @Getter @Setter @NoArgsConstructor
public class RiskCase {
  @Id @GeneratedValue(strategy=GenerationType.UUID) private UUID id;
  @OneToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="record_id") private EmotionRecord record;
  @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="student_id") private User student;
  @Enumerated(EnumType.STRING) @Column(name="risk_level", nullable=false) private RiskLevel riskLevel;
  @Enumerated(EnumType.STRING) @Column(name="source_visibility", nullable=false) private Visibility sourceVisibility;
  @Column(name="public_content_snapshot", columnDefinition="text") private String publicContentSnapshot;
  @Enumerated(EnumType.STRING) @Column(nullable=false) private CaseStatus status = CaseStatus.OPEN;
  @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="assignee_id") private User assignee;
  @Column(name="closure_note", columnDefinition="text") private String closureNote;
  @Column(name="student_declined",nullable=false) private boolean studentDeclined;
  @Column(name="false_positive",nullable=false) private boolean falsePositive;
  @Column(name="created_at", insertable=false, updatable=false) private Instant createdAt;
  @Column(name="updated_at", insertable=false) private Instant updatedAt;
  @PreUpdate void touch(){updatedAt=Instant.now();}
}
