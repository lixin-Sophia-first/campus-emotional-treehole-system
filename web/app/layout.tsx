import type { Metadata } from "next";
import "./globals.css";
import "./actions.css";
import { AppShell } from "@/components/AppShell";
export const metadata:Metadata={title:"心屿 · 校园树洞",description:"一个安全、温柔的校园情绪支持空间"};
export default function RootLayout({children}:{children:React.ReactNode}){return <html lang="zh-CN"><body><AppShell>{children}</AppShell></body></html>}
