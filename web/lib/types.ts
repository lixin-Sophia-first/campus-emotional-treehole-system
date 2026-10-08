export type Role = "ADMIN" | "TEACHER" | "STUDENT";
export type RiskLevel = "LOW" | "MEDIUM" | "HIGH" | "CRITICAL";
export type Visibility = "PUBLIC" | "PRIVATE";
export interface User { id:string; email:string; name:string; role:Role }
export interface EmotionRecord { id:string; title:string; content:string; visibility:Visibility; emotion:string; intensity:number; tags:string[]; alias:string; anonymous:boolean; riskLevel:RiskLevel|null; supportCard:string; recommendationEligible:boolean; moderationStatus:"PUBLISHED"|"PENDING_REVIEW"|"HIDDEN"|"DELETED"; createdAt:string; mine:boolean;likeCount:number;commentCount:number;liked:boolean;bookmarked:boolean }
export interface RiskCase { id:string; riskLevel:RiskLevel|null; sourceVisibility:Visibility; publicContent:string|null; status:string; assigneeId:string|null; assigneeName:string|null; mine:boolean; identityAvailable:boolean;identityRequestPending:boolean;studentDeclined:boolean;falsePositive:boolean;closureNote:string|null;createdAt:string }
export interface TeacherApplication { id:string;email:string;name:string;employeeNumber:string;department:string;statement:string;status:string }
export interface Resource {id:string;title:string;summary:string;content:string;type:string;author:string;version:number;active:boolean;mine:boolean;updatedAt:string}
