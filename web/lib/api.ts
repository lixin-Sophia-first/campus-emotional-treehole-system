export class ApiError extends Error { constructor(public status:number,message:string){super(message)} }
function cookie(name:string){if(typeof document==="undefined")return "";return document.cookie.split("; ").find(x=>x.startsWith(name+"="))?.split("=").slice(1).join("=")??""}
async function request(path:string,init?:RequestInit){const method=(init?.method??"GET").toUpperCase();const csrf=!(["GET","HEAD","OPTIONS"].includes(method))?cookie("treehole_csrf"):"";return fetch(`/api/v1${path}`,{...init,credentials:"include",headers:{"Content-Type":"application/json",...(csrf?{"X-CSRF-Token":decodeURIComponent(csrf)}:{}),...(init?.headers??{})}})}
export async function api<T>(path:string,init?:RequestInit):Promise<T>{
  let res:Response;try{res=await request(path,init)}catch{throw new ApiError(0,"Cannot reach the Tree Hole server. Make sure Docker is running, then try again.")}
  if(res.status===401&&!path.startsWith("/auth/")){const refreshed=await request("/auth/refresh",{method:"POST"});if(refreshed.ok)res=await request(path,init)}
  if(!res.ok){const body=await res.json().catch(()=>({message:""}));const fallback=res.status===403?"This action was blocked. Refresh the page and try again.":res.status===401?"Your session has expired. Please sign in again.":res.status>=500?"The server could not complete this request. Please try again; if it happens again, contact the administrator.":`The request could not be completed (${res.status}).`;throw new ApiError(res.status,body.message||fallback)}
  if(res.status===204||res.headers.get("content-length")==="0") return undefined as T;
  return res.json();
}
