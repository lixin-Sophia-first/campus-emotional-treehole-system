"use client";
import{useEffect,useState}from "react";import Link from "next/link";import{api}from "@/lib/api";
export default function Verify(){const[state,setState]=useState("正在验证邮箱……");useEffect(()=>{const token=new URLSearchParams(location.search).get("token");if(!token){setState("验证链接不完整");return}api(`/auth/verify?token=${encodeURIComponent(token)}`,{method:"POST"}).then(()=>setState("邮箱验证成功，现在可以登录了。"),()=>setState("验证链接无效或已过期。"))},[]);return <div className="authWrap card"><div className="eyebrow">EMAIL VERIFICATION</div><h1>验证邮箱</h1><div className="notice success">{state}</div><Link className="button" href="/login">返回登录</Link></div>}

