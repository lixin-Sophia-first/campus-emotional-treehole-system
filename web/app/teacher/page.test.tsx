import {fireEvent,render,screen,waitFor,within} from "@testing-library/react";
import {beforeEach,expect,it,vi} from "vitest";
import Teacher from "./page";

const apiMock=vi.fn();
vi.mock("@/lib/api",async importOriginal=>({...await importOriginal<typeof import("@/lib/api")>(),api:(...args:unknown[])=>apiMock(...args)}));

beforeEach(()=>{apiMock.mockReset();});

it("shows approved identity automatically and expands care inside its own case",async()=>{
  const riskCase={id:"case-1",riskLevel:"HIGH",sourceVisibility:"PRIVATE",publicContent:null,status:"IN_PROGRESS",assigneeId:"teacher-1",assigneeName:"Teacher",mine:false,identityAvailable:true,identityRequestPending:false,studentDeclined:false,falsePositive:false,closureNote:null,createdAt:"2026-10-08T00:00:00Z"};
  apiMock.mockImplementation((path:string)=>{
    if(path==="/auth/me")return Promise.resolve({id:"teacher-1",email:"teacher@example.test",name:"Teacher",role:"TEACHER"});
    if(path==="/risk-cases")return Promise.resolve([riskCase]);
    if(path==="/risk-cases/case-1/student-identity")return Promise.resolve({name:"Test Student",email:"student@example.test"});
    if(path==="/risk-cases/case-1/messages")return Promise.resolve([]);
    throw new Error(`Unexpected API path: ${path}`);
  });
  render(<Teacher/>);
  fireEvent.click(await screen.findByRole("button",{name:"My active cases"}));
  const card=screen.getByText("Private entry content is hidden to protect the student.").closest("article");
  expect(card).not.toBeNull();
  await waitFor(()=>expect(within(card!).getByText(/Test Student · student@example.test/)).toBeInTheDocument());
  expect(within(card!).queryByRole("button",{name:"View authorised identity"})).not.toBeInTheDocument();
  fireEvent.click(within(card!).getByRole("button",{name:"Open care workspace"}));
  await waitFor(()=>expect(within(card!).getByRole("region",{name:"Case workspace"})).toBeInTheDocument());
  expect(apiMock).toHaveBeenCalledWith("/risk-cases/case-1/student-identity");
});

it("lets a teacher claim a case before automatic assignment",async()=>{
  const riskCase={id:"case-2",riskLevel:"HIGH",sourceVisibility:"PRIVATE",publicContent:null,status:"OPEN",assigneeId:null,assigneeName:null,mine:false,identityAvailable:false,identityRequestPending:false,studentDeclined:false,falsePositive:false,closureNote:null,createdAt:"2026-10-08T00:00:00Z"};
  apiMock.mockImplementation((path:string)=>{
    if(path==="/auth/me")return Promise.resolve({id:"teacher-1",email:"teacher@example.test",name:"Teacher",role:"TEACHER"});
    if(path==="/risk-cases")return Promise.resolve([riskCase]);
    if(path==="/risk-cases/case-2/claim")return Promise.resolve({...riskCase,status:"IN_PROGRESS",assigneeId:"teacher-1"});
    throw new Error(`Unexpected API path: ${path}`);
  });
  render(<Teacher/>);
  fireEvent.click(await screen.findByRole("button",{name:"Claim case"}));
  await waitFor(()=>expect(apiMock).toHaveBeenCalledWith("/risk-cases/case-2/claim",{method:"POST",body:undefined}));
});
