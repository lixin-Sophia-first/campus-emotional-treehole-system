package cn.campus.treehole.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name="case_messages") @Getter @Setter @NoArgsConstructor
public class CaseMessage {
  @Id @GeneratedValue(strategy=GenerationType.UUID) private UUID id;
  @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="case_id") private RiskCase riskCase;
  @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="sender_id") private User sender;
  @Column(nullable=false, columnDefinition="text") private String content;
  @Column(name="created_at", insertable=false, updatable=false) private Instant createdAt;
}

