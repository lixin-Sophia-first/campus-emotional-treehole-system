"use client";
import {useState} from "react";

type Props={label:string;value:string;onChange:(value:string)=>void;minLength?:number};

export default function PasswordField({label,value,onChange,minLength=8}:Props){
 const[visible,setVisible]=useState(false);
 return <div className="field"><label>{label}</label><input type={visible?"text":"password"} minLength={minLength} value={value} onChange={e=>onChange(e.target.value)} required/><label className="passwordToggle"><input type="checkbox" checked={visible} onChange={e=>setVisible(e.target.checked)}/><span>Show password</span></label></div>
}
