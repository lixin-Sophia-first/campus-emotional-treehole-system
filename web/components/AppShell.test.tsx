import {render,screen,waitFor} from "@testing-library/react";
import {beforeEach,describe,expect,it,vi} from "vitest";
import {AppShell} from "./AppShell";

const apiMock=vi.fn();
vi.mock("@/lib/api",()=>({api:(...args:unknown[])=>apiMock(...args)}));

describe("AppShell role navigation",()=>{
  beforeEach(()=>apiMock.mockReset());
  it("shows student-only destinations after authentication",async()=>{
    apiMock.mockResolvedValue({id:"1",email:"student@example.test",name:"Student",role:"STUDENT"});
    render(<AppShell><div>Page content</div></AppShell>);
    await waitFor(()=>expect(screen.getByText("My emotions")).toBeInTheDocument());
    expect(screen.getByText("Bookmarks")).toBeInTheDocument();
    expect(screen.queryByText("Admin console")).not.toBeInTheDocument();
  });
  it("offers account creation to a visitor",async()=>{
    apiMock.mockResolvedValue(null);
    render(<AppShell><div>Page content</div></AppShell>);
    await waitFor(()=>expect(screen.getByText("Create account")).toBeInTheDocument());
    expect(screen.getByText("Sign in")).toBeInTheDocument();
  });
});
