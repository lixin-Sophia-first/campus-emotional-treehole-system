import type { RiskLevel } from "./types";
export function supportMessage(level:RiskLevel){return ({LOW:"谢谢你认真记录此刻的感受。给自己一点休息的空间。",MEDIUM:"试试 5-4-3-2-1 落地练习，慢慢把注意力带回当下。",HIGH:"你不需要独自承担这些。可以看看支持资源，或接受认证教师的匿名关怀。",CRITICAL:"你的安全最重要。请先远离可能伤害自己的物品，并立即联系可信赖的人或紧急支持服务。"})[level]}

