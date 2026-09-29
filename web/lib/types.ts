export type Role = "ADMIN" | "TEACHER" | "STUDENT";
export type RiskLevel = "LOW" | "MEDIUM" | "HIGH" | "CRITICAL";
export type Visibility = "PUBLIC" | "PRIVATE";
export interface User { id:string; email:string; name:string; role:Role }
export interface EmotionRecord { id:string; content:string; visibility:Visibility; emotion:string; intensity:number; tags:string[]; alias:string; riskLevel:RiskLevel; supportCard:string; recommendationEligible:boolean; createdAt:string; mine:boolean }
export interface RiskCase { id:string; riskLevel:RiskLevel; sourceVisibility:Visibility; publicContent:string|null; status:string; assigneeId:string|null; assigneeName:string|null; mine:boolean; identityAvailable:boolean; createdAt:string }
export interface TeacherApplication { id:string;email:string;name:string;employeeNumber:string;department:string;statement:string;status:string }
export interface Resource {id:string;title:string;summary:string;content:string;type:string;author:string;updatedAt:string}

