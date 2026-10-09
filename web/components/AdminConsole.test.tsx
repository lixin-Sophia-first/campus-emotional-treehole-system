import {fireEvent,render,screen,waitFor} from "@testing-library/react";
import {beforeEach,expect,it,vi} from "vitest";
import AdminConsole from "./AdminConsole";

const apiMock=vi.fn();
vi.mock("@/lib/api",async importOriginal=>({...await importOriginal<typeof import("@/lib/api")>(),api:(...args:unknown[])=>apiMock(...args)}));
beforeEach(()=>apiMock.mockReset());

it("shows local private analysis without an external retry action",async()=>{
  apiMock.mockImplementation((path:string)=>{
    if(path==="/admin/ai-status")return Promise.resolve({externalModelConfigured:false,privateEntriesLocalOnly:true});
    if(path==="/admin/ai-tasks")return Promise.resolve([{id:"task-1",status:"SKIPPED",attempts:0,last_error:"",visibility:"PRIVATE",risk_level:"HIGH",created_at:"2026-10-08T00:00:00Z"}]);
    if(path==="/admin/ai-tasks/task-1/results")return Promise.resolve([{source:"LOCAL",primary_emotion:"Sad",intensity:8,risk_level:"HIGH",explicit_plan:false,imminent_timing:false,means_present:false,violation_category:"NONE",confidence:0.8,support_card:"SEEK_SUPPORT",reason:"Severe hopelessness",created_at:"2026-10-08T00:00:00Z"}]);
    return Promise.resolve([]);
  });
  render(<AdminConsole section="ai"/>);
  expect(await screen.findByText(/Local English risk rules remain active/)).toBeInTheDocument();
  expect(screen.getByText(/Private entry: local rules only/)).toBeInTheDocument();
  expect(screen.getByRole("button",{name:"Run local rules again"})).toBeInTheDocument();
  expect(screen.queryByRole("button",{name:"Reanalyse public post"})).not.toBeInTheDocument();
  fireEvent.click(screen.getByRole("button",{name:"View analysis results"}));
  await waitFor(()=>expect(screen.getByText(/Local rules · HIGH/)).toBeInTheDocument());
  expect(screen.getByText("Severe hopelessness")).toBeInTheDocument();
});

it("keeps model-suggested rules inactive until separate admin confirmation and activation",async()=>{
  apiMock.mockImplementation((path:string,init?:RequestInit)=>{
    if(path==="/admin/ai-status")return Promise.resolve({externalModelConfigured:true,privateEntriesLocalOnly:true});
    if(path==="/admin/risk-rule-candidates")return Promise.resolve([{id:"candidate-1",version:1,record_id:"record-1",local_risk:"LOW",model_risk:"HIGH",status:"DETECTED",phrase:null,rationale:null,title:"A public post",content:"A synthetic example",created_at:"2026-10-08T00:00:00Z"}]);
    if(init?.method==="POST")return Promise.resolve(undefined);
    return Promise.resolve([]);
  });
  render(<AdminConsole section="ai"/>);
  expect(await screen.findByText(/Rule gap #1: local LOW → model HIGH/)).toBeInTheDocument();
  expect(screen.getByRole("button",{name:"Confirm this risk"})).toBeInTheDocument();
  expect(screen.queryByRole("button",{name:"Activate rule"})).not.toBeInTheDocument();
  fireEvent.click(screen.getByRole("button",{name:"Confirm this risk"}));
  await waitFor(()=>expect(apiMock).toHaveBeenCalledWith("/admin/risk-rule-candidates/candidate-1/confirm",{method:"POST",body:undefined}));
});
