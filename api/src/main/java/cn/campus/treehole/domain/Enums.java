package cn.campus.treehole.domain;

public final class Enums {
  private Enums() {}
  public enum Role { ADMIN, TEACHER, STUDENT }
  public enum UserStatus { UNVERIFIED, ACTIVE, DISABLED }
  public enum Visibility { PUBLIC, PRIVATE }
  public enum RiskLevel { LOW, MEDIUM, HIGH, CRITICAL }
  public enum ModerationStatus { PUBLISHED, PENDING_REVIEW, HIDDEN, DELETED }
  public enum CaseStatus { OPEN, IN_PROGRESS, CLOSURE_PROPOSED, CLOSED }
  public enum ApplicationStatus { PENDING, APPROVED, REJECTED }
  public enum AccessStatus { PENDING, APPROVED, REJECTED }
}
