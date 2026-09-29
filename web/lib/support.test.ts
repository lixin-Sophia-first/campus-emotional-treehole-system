import {describe,expect,it} from "vitest";
import {supportMessage} from "./support";
describe("supportMessage",()=>{it("uses urgent language only for critical risk",()=>{expect(supportMessage("CRITICAL")).toContain("safety matters most");expect(supportMessage("LOW")).not.toContain("emergency")})});
