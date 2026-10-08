"use client";

import {FormEvent,useEffect,useMemo,useState} from "react";
import {api,ApiError} from "@/lib/api";
import type {RiskCase,User} from "@/lib/types";

type Message={id:string;senderName:string;content:string;createdAt:string};
type Identity={name:string;email:string};

export default function Teacher(){
  const [user,setUser]=useState<User|null>(null);
  const [cases,setCases]=useState<RiskCase[]>([]);
  const [filter,setFilter]=useState("queue");
  const [selectedId,setSelectedId]=useState<string|null>(null);
  const [error,setError]=useState("");

  async function load(){
    try{
      const [u,found]=await Promise.all([api<User>("/auth/me"),api<RiskCase[]>("/risk-cases")]);
      if(u.role!=="TEACHER"){location.href="/login";return;}
      setUser(u);setCases(found);
      const target=new URLSearchParams(location.search).get("case");
      const match=found.find(c=>c.id===target&&c.assigneeId===u.id);
      if(match){
        setFilter(match.status==="CLOSED"?"closed":match.status==="CLOSURE_PROPOSED"?"closure":"mine");
        setSelectedId(match.id);
        history.replaceState(null,"","/teacher");
      }
    }catch(e){
      if(e instanceof ApiError&&e.status===401){location.href="/login";return;}
      setError(e instanceof ApiError?e.message:"Unable to load risk cases.");
    }
  }
  useEffect(()=>{load()},[]);
  const pendingIdentity=cases.some(c=>c.assigneeId===user?.id&&c.identityRequestPending);
  useEffect(()=>{
    if(!pendingIdentity)return;
    const timer=window.setInterval(()=>{api<RiskCase[]>("/risk-cases").then(setCases).catch(()=>{});},5000);
    return()=>window.clearInterval(timer);
  },[pendingIdentity]);

  async function act(path:string,body?:unknown){
    try{await api(path,{method:"POST",body:body?JSON.stringify(body):undefined});setError("");await load();}
    catch(e){setError(e instanceof ApiError?e.message:"Action failed");}
  }

  const shown=useMemo(()=>cases.filter(c=>filter==="queue"?!c.assigneeId&&c.status!=="CLOSED":filter==="mine"?c.assigneeId===user?.id&&c.status!=="CLOSED"&&c.status!=="CLOSURE_PROPOSED":filter==="closure"?c.assigneeId===user?.id&&c.status==="CLOSURE_PROPOSED":c.assigneeId===user?.id&&c.status==="CLOSED"),[cases,filter,user]);

  return <div className="dashboard">
    <aside className="card sidebar">
      <div className="eyebrow">TEACHER</div><h3>Care workspace</h3>
      {[["queue","Unclaimed queue"],["mine","My active cases"],["closure","Awaiting admin review"],["closed","Closed cases"]].map(([key,label])=><button className={`sidebarButton ${filter===key?"active":""}`} onClick={()=>{setFilter(key);setSelectedId(null);}} key={key}>{label}</button>)}
    </aside>
    <section>
      <div className="eyebrow">CARE WITH BOUNDARIES</div>
      <h1 className="sectionTitle" style={{fontSize:38}}>Risk care queue</h1>
      <p className="subtle">Claim an unassigned case to start anonymous care. Cases still unclaimed after 24 hours are assigned automatically. Private entry content is never disclosed.</p>
      {error&&<div className="notice" role="alert">{error}</div>}
      <div className="statGrid">
        <div className="card stat"><span className="muted">Unclaimed</span><strong>{cases.filter(c=>!c.assigneeId&&c.status!=="CLOSED").length}</strong></div>
        <div className="card stat"><span className="muted">Assigned to me</span><strong>{cases.filter(c=>c.assigneeId===user?.id&&c.status!=="CLOSED").length}</strong></div>
        <div className="card stat"><span className="muted">Critical</span><strong>{cases.filter(c=>c.riskLevel==="CRITICAL"&&c.status!=="CLOSED").length}</strong></div>
      </div>
      {shown.map(c=>{
        const mine=c.assigneeId===user?.id,expanded=selectedId===c.id;
        return <article className={`card case ${c.riskLevel==="CRITICAL"?"critical":""}`} key={c.id}>
          <div className="caseTop"><div><span className={`risk ${c.riskLevel}`}>{c.riskLevel}</span><h3>{c.sourceVisibility==="PRIVATE"?"🔒 Private-entry risk signal":"Public treehole risk case"}</h3></div><span className="chip caseStatus">{caseStatusLabel(c.status)}</span></div>
          <p className="caseContent">{c.publicContent??"Private entry content is hidden to protect the student."}</p>
          <p className="subtle">Assignee: {c.assigneeName??"Not assigned yet"}{c.studentDeclined?" · Student declined further conversation":""}</p>
          {mine&&c.identityAvailable&&<IdentityDetails caseId={c.id}/>}
          {mine&&c.riskLevel==="HIGH"&&c.identityRequestPending&&<p className="subtle">Identity access request pending administrator approval.</p>}
          <div className="caseActions">
            {!c.assigneeId&&<button className="button small" onClick={()=>act(`/risk-cases/${c.id}/claim`)}>Claim case</button>}
            {mine&&c.riskLevel==="HIGH"&&!c.identityAvailable&&!c.identityRequestPending&&<button className="button secondary small" onClick={()=>act(`/risk-cases/${c.id}/identity-request`,{reason:"Identity is required to continue proportionate risk support."})}>Request identity access</button>}
            {mine&&<button className="button secondary small" aria-expanded={expanded} aria-controls={`care-workspace-${c.id}`} onClick={()=>setSelectedId(expanded?null:c.id)}>{expanded?"Close care workspace":"Open care workspace"}</button>}
          </div>
          {mine&&expanded&&<div id={`care-workspace-${c.id}`}><CaseWorkspace c={c} refresh={load}/></div>}
        </article>;
      })}
      {shown.length===0&&<div className="card empty">No cases in this view.</div>}
    </section>
  </div>;
}

function caseStatusLabel(status:string){return {OPEN:"Waiting for teacher",IN_PROGRESS:"In progress",CLOSURE_PROPOSED:"Waiting for admin review",CLOSED:"Closed"}[status]??status;}

function IdentityDetails({caseId}:{caseId:string}){
  const [identity,setIdentity]=useState<Identity|null>(null);
  const [error,setError]=useState("");
  useEffect(()=>{
    let active=true;
    api<Identity>(`/risk-cases/${caseId}/student-identity`).then(value=>{if(active)setIdentity(value);}).catch(e=>{if(active)setError(e instanceof ApiError?e.message:"Identity could not be loaded.");});
    return()=>{active=false;};
  },[caseId]);
  if(error)return <div className="notice" role="alert">{error}</div>;
  return <p className="identityDetail"><strong>Authorised student identity:</strong> {identity?`${identity.name} · ${identity.email}`:"Loading…"}</p>;
}

function CaseWorkspace({c,refresh}:{c:RiskCase;refresh:()=>Promise<void>}){
  const [messages,setMessages]=useState<Message[]>([]);
  const [text,setText]=useState("");
  const [note,setNote]=useState("");
  const [error,setError]=useState("");
  async function load(){setMessages(await api<Message[]>(`/risk-cases/${c.id}/messages`));}
  useEffect(()=>{load().catch(e=>setError(e instanceof ApiError?e.message:"Unable to load messages."));},[c.id]);
  async function send(e:FormEvent){e.preventDefault();try{await api(`/risk-cases/${c.id}/messages`,{method:"POST",body:JSON.stringify({content:text})});setText("");await load();}catch(err){setError(err instanceof ApiError?err.message:"Message could not be sent.");}}
  async function followup(){if(!note.trim())return;try{await api(`/risk-cases/${c.id}/followups`,{method:"POST",body:JSON.stringify({note})});setNote("");alert("Follow-up note saved.");}catch(err){setError(err instanceof ApiError?err.message:"Follow-up could not be saved.");}}
  async function propose(){if(!note.trim()){setError("Add a closure recommendation first.");return;}try{await api(`/risk-cases/${c.id}/propose-closure`,{method:"POST",body:JSON.stringify({note})});setNote("");await refresh();}catch(err){setError(err instanceof ApiError?err.message:"Closure recommendation could not be submitted.");}}
  return <section className="caseWorkspace" aria-label="Case workspace">
    <h3>Case workspace</h3><h4>Anonymous care conversation</h4>
    {error&&<div className="notice" role="alert">{error}</div>}
    {messages.map(m=><p key={m.id}><strong>{m.senderName}: </strong>{m.content}</p>)}
    {c.studentDeclined?<div className="notice">The student declined further conversation. Case notes and administrative review remain available.</div>:c.status!=="CLOSED"&&<form onSubmit={send} className="row"><input value={text} onChange={e=>setText(e.target.value)} placeholder="Respond respectfully without diagnosing…" required/><button className="button small">Send</button></form>}
    {c.status!=="CLOSED"&&<><div className="field"><label>Private follow-up or closure recommendation</label><textarea value={note} onChange={e=>setNote(e.target.value)} placeholder="Record actions, observations and next steps without copying private entry content."/></div><div className="caseActions"><button className="button secondary small" onClick={followup}>Save follow-up</button><button className="button small" onClick={propose}>Propose closure</button></div></>}
  </section>;
}
