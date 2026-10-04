"use client";
import {useState} from "react";

type Props={label:string;value:string;onChange:(value:string)=>void;minLength?:number};

export default function PasswordField({label,value,onChange,minLength=8}:Props){
 const[visible,setVisible]=useState(false);
 return <div className="field"><label>{label}</label><div className="passwordInput"><input type={visible?"text":"password"} minLength={minLength} value={value} onChange={e=>onChange(e.target.value)} required/><button type="button" aria-label={visible?"Hide password":"Show password"} aria-pressed={visible} onClick={()=>setVisible(x=>!x)}>{visible?"Hide":"Show"}</button></div></div>
}
