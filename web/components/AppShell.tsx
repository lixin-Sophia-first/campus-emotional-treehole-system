"use client";
import Link from "next/link";
import { useEffect,useState } from "react";
import { api } from "@/lib/api";
import type { User } from "@/lib/types";
export function AppShell({children}:{children:React.ReactNode}){
 const [user,setUser]=useState<User|null>(null);
 useEffect(()=>{api<User>("/auth/me").then(setUser).catch(()=>setUser(null))},[]);
 async function logout(){await api("/auth/logout",{method:"POST"});location.href="/login"}
 return <><header className="topbar"><Link href="/" className="brand"><span>心屿</span><small>CAMPUS TREEHOLE</small></Link><nav><Link href="/">树洞广场</Link>{user?.role==="STUDENT"&&<><Link href="/timeline">我的情绪</Link><Link href="/care">关怀会话</Link></>}<Link href="/resources">支持资源</Link>{user?.role==="TEACHER"&&<Link href="/teacher">教师工作台</Link>}{user?.role==="ADMIN"&&<Link href="/admin">管理后台</Link>}</nav><div className="account">{user?<><span className={`role ${user.role.toLowerCase()}`}>{user.role==="STUDENT"?"学生":user.role==="TEACHER"?"教师":"管理员"}</span><span>{user.name}</span><button className="linkButton" onClick={logout}>退出</button></>:<Link className="button small" href="/login">登录</Link>}</div></header><main className="page">{children}</main><footer>心屿不是医疗服务。如果你正面临迫切危险，请立即联系当地紧急服务或可信赖的人。</footer></>
}
