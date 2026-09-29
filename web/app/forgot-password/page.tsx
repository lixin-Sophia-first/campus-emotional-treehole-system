"use client";
import{FormEvent,useState}from "react";import{api}from "@/lib/api";
export default function Forgot(){const[email,setEmail]=useState(""),[sent,setSent]=useState(false);async function submit(e:FormEvent){e.preventDefault();await api("/auth/forgot-password",{method:"POST",body:JSON.stringify({email})});setSent(true)}return <div className="authWrap card"><div className="eyebrow">ACCOUNT RECOVERY</div><h1>找回密码</h1><p className="subtle">输入注册邮箱。若账号存在，重置链接会发送到本地邮件箱。</p><form onSubmit={submit}><div className="field"><label>邮箱</label><input type="email" value={email} onChange={e=>setEmail(e.target.value)} required/></div>{sent&&<div className="notice success">请求已处理，请检查本地邮件箱。</div>}<button className="button">发送重置链接</button></form></div>}

