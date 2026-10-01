import type { Metadata } from "next";
import "./globals.css";
import { SessionProvider } from "@/components/providers/SessionProvider";
import { CartProvider } from "@/components/providers/CartProvider";
import Navbar from "@/components/site/Navbar";
import Footer from "@/components/site/Footer";

export const metadata: Metadata = {
  metadataBase: new URL(process.env.NEXT_PUBLIC_SITE_URL ?? "http://localhost:3000"),
  title: {
    default: "StudyVault — Premium Programming Notes & Study Material",
    template: "%s · StudyVault",
  },
  description:
    "Hand-crafted programming notes, e-books and cheat sheets — Java, Spring Boot, SQL, DSA, System Design and more. Written by engineers, for engineers.",
  authors: [{ name: "Krrish" }],
  creator: "Krrish",
  openGraph: {
    type: "website",
    siteName: "StudyVault",
  },
  twitter: { card: "summary_large_image" },
  robots: { index: true, follow: true },
};

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="en">
      <body className="antialiased">
        <SessionProvider>
          <CartProvider>
            <Navbar />
            <main className="min-h-[70vh]">{children}</main>
            <Footer />
          </CartProvider>
        </SessionProvider>
      </body>
    </html>
  );
}
