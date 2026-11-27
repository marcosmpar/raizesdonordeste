import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  // gera um servidor enxuto em .next/standalone (usado pelo Dockerfile)
  output: "standalone",
  poweredByHeader: false,
  turbopack: {
    rules: {
      "*.css": {
        loaders: ["@tailwindcss/turbopack"],
        as: "*.css",
      },
    },
  },
};

export default nextConfig;
