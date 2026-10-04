"use client";
import Link from "next/link";
import { useEffect,useState } from "react";
import { api } from "@/lib/api";
import type { User } from "@/lib/types";
export function AppShell({children}:{children:React.ReactNode}){
 const [user,setUser]=useState<User|null>(null);
 useEffect(()=>{api<User>("/auth/me").then(setUser).catch(()=>setUser(null))},[]);
 async function logout(){await api("/auth/logout",{method:"POST"});location.href="/login"}
 return <><header className="topbar"><Link href="/" className="brand"><span>Tree Hole</span><small>CAMPUS EMOTIONAL SUPPORT</small></Link><nav><Link href="/">Community</Link>{user?.role==="STUDENT"&&<><Link href="/timeline">My emotions</Link><Link href="/bookmarks">Bookmarks</Link><Link href="/care">Care conversations</Link></>}<Link href="/resources">Support resources</Link>{user&&<Link href="/notifications">Notifications</Link>}{user?.role==="TEACHER"&&<Link href="/teacher">Teacher workspace</Link>}{user?.role==="ADMIN"&&<Link href="/admin">Admin console</Link>}</nav><div className="account">{user?<><span className={`role ${user.role.toLowerCase()}`}>{user.role}</span><span>{user.name}</span><button className="linkButton" onClick={logout}>Sign out</button></>:<><Link className="headerSignup" href="/login?mode=register">Create account</Link><Link className="button small" href="/login">Sign in</Link></>}</div></header><main className="page">{children}</main><footer>Tree Hole is not a medical service. If you are in immediate danger, contact local emergency services or someone you trust.</footer></>
}
