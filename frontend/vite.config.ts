import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";
export default defineConfig({
  plugins: [react()],
  server: { proxy: { "/api": "http://127.0.0.1:8181" } },
  preview: {
    proxy: { "/api": process.env.CHAMBA_API_PROXY || "http://127.0.0.1:8181" },
  },
});
