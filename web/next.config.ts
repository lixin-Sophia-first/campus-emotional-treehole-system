import type { NextConfig } from "next";
const api = process.env.API_INTERNAL_URL ?? "http://localhost:8080";
const config: NextConfig = {
  output: "standalone",
  async rewrites() { return [{ source: "/api/:path*", destination: `${api}/api/:path*` }]; },
};
export default config;

