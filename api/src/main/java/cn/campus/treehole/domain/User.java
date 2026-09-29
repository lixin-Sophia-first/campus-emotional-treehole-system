package cn.campus.treehole.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;
import static cn.campus.treehole.domain.Enums.*;

@Entity @Table(name="users") @Getter @Setter @NoArgsConstructor
public class User {
  @Id @GeneratedValue(strategy=GenerationType.UUID) private UUID id;
  @Column(nullable=false, unique=true) private String email;
  @Column(name="password_hash", nullable=false) private String passwordHash;
  @Column(name="display_name", nullable=false) private String displayName;
  @Enumerated(EnumType.STRING) @Column(nullable=false) private Role role;
  @Enumerated(EnumType.STRING) @Column(nullable=false) private UserStatus status = UserStatus.ACTIVE;
  private String department;
  @Column(name="employee_number") private String employeeNumber;
  @Column(name="created_at", insertable=false, updatable=false) private Instant createdAt;
}

