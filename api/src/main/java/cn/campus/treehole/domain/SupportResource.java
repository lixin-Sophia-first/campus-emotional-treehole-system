package cn.campus.treehole.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name="support_resources") @Getter @Setter @NoArgsConstructor
public class SupportResource {
  @Id @GeneratedValue(strategy=GenerationType.UUID) private UUID id;
  @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="author_id") private User author;
  @Column(nullable=false) private String title;
  @Column(nullable=false, columnDefinition="text") private String summary;
  @Column(nullable=false, columnDefinition="text") private String content;
  @Column(name="resource_type", nullable=false) private String resourceType;
  @Column(nullable=false) private boolean active = true;
  @Column(nullable=false) private int version = 1;
  @Column(name="created_at", insertable=false, updatable=false) private Instant createdAt;
  @Column(name="updated_at", insertable=false, updatable=false) private Instant updatedAt;
}
