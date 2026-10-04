import {expect,test} from "@playwright/test";

test("a visitor can open the public community",async({page})=>{
  await page.goto("/");
  await expect(page.getByRole("link",{name:"Tree Hole"})).toBeVisible();
  await expect(page.getByRole("link",{name:"Create account"})).toBeVisible();
  await expect(page.getByText("Latest",{exact:true})).toBeVisible();
});
