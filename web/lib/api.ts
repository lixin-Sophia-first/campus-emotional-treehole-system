export class ApiError extends Error { constructor(public status:number,message:string){super(message)} }
export async function api<T>(path:string,init?:RequestInit):Promise<T>{
  const res=await fetch(`/api/v1${path}`,{...init,credentials:"include",headers:{"Content-Type":"application/json",...(init?.headers??{})}});
  if(!res.ok){const body=await res.json().catch(()=>({message:"Request failed"}));throw new ApiError(res.status,body.message??"Request failed")}
  if(res.status===204||res.headers.get("content-length")==="0") return undefined as T;
  return res.json();
}
