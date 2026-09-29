package cn.campus.treehole.domain;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;
import static cn.campus.treehole.domain.Enums.*;
@Entity @Table(name="identity_access_requests") @Getter @Setter @NoArgsConstructor
public class IdentityAccessRequest {
  @Id @GeneratedValue(strategy=GenerationType.UUID) private UUID id;
  @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="case_id") private RiskCase riskCase;
  @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="teacher_id") private User teacher;
  @Column(nullable=false,columnDefinition="text") private String reason;
  @Enumerated(EnumType.STRING) @Column(nullable=false) private AccessStatus status=AccessStatus.PENDING;
  @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="reviewed_by") private User reviewedBy;
  @Column(name="reviewed_at") private Instant reviewedAt;
  @Column(name="created_at",insertable=false,updatable=false) private Instant createdAt;
}

