import type { Metadata } from "next";
import "./globals.css";
import "./actions.css";
import { AppShell } from "@/components/AppShell";
export const metadata:Metadata={title:"Tree Hole · Campus Emotional Support",description:"A safe and supportive emotional space for campus communities"};
export default function RootLayout({children}:{children:React.ReactNode}){return <html lang="en"><body><AppShell>{children}</AppShell></body></html>}
