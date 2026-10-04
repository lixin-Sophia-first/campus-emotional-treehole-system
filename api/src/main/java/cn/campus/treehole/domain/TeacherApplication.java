package cn.campus.treehole.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;
import static cn.campus.treehole.domain.Enums.*;

@Entity @Table(name="teacher_applications") @Getter @Setter @NoArgsConstructor
public class TeacherApplication {
  @Id @GeneratedValue(strategy=GenerationType.UUID) private UUID id;
  @Column(nullable=false) private String email;
  @Column(name="password_hash", nullable=false) private String passwordHash;
  @Column(nullable=false) private String name;
  @Column(name="employee_number", nullable=false) private String employeeNumber;
  @Column(nullable=false) private String department;
  @Column(nullable=false, columnDefinition="text") private String statement;
  @Enumerated(EnumType.STRING) @Column(nullable=false) private ApplicationStatus status = ApplicationStatus.PENDING;
  @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="reviewed_by") private User reviewedBy;
  @Column(name="reviewed_at") private Instant reviewedAt;
  @Column(name="created_at", insertable=false, updatable=false) private Instant createdAt;
}
