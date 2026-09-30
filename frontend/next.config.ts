import type { NextConfig } from "next";

/**
 * Content-Security-Policy: allows the Razorpay checkout script/iframe and the
 * API origin; everything else is locked down. 'unsafe-inline' styles are
 * required by Next.js and React Three Fiber; scripts stay nonce/hash-less for
 * simplicity but connect-src and frame-src are tightly scoped.
 */
const apiUrl = process.env.NEXT_PUBLIC_API_URL ?? "";
const csp = [
  "default-src 'self'",
  `script-src 'self' 'unsafe-inline' https://checkout.razorpay.com`,
  `style-src 'self' 'unsafe-inline'`,
  "img-src 'self' data: blob: https://*.razorpay.com",
  `connect-src 'self' ${apiUrl} https://api.razorpay.com`,
  "frame-src https://api.razorpay.com https://checkout.razorpay.com",
  "font-src 'self' data:",
  "object-src 'none'",
  "base-uri 'self'",
  "frame-ancestors 'none'",
].join("; ");

const nextConfig: NextConfig = {
  reactStrictMode: true,
  output: "standalone",
  images: { unoptimized: true },
  async headers() {
    return [
      {
        source: "/(.*)",
        headers: [
          { key: "Content-Security-Policy", value: csp },
          { key: "X-Content-Type-Options", value: "nosniff" },
          { key: "Referrer-Policy", value: "strict-origin-when-cross-origin" },
          { key: "X-Frame-Options", value: "DENY" },
          { key: "Permissions-Policy", value: "camera=(), microphone=(), geolocation=()" },
          { key: "Strict-Transport-Security", value: "max-age=63072000; includeSubDomains; preload" },
        ],
      },
    ];
  },
};

export default nextConfig;
