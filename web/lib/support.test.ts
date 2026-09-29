import {describe,expect,it} from "vitest";
import {supportMessage} from "./support";
describe("supportMessage",()=>{it("uses urgent language only for critical risk",()=>{expect(supportMessage("CRITICAL")).toContain("安全最重要");expect(supportMessage("LOW")).not.toContain("紧急")})});
