import { Suspense } from "react";
import type { Metadata } from "next";
import ProductsClient from "./ProductsClient";

export const metadata: Metadata = {
  title: "Catalog — programming notes, e-books & cheat sheets",
  description:
    "Browse StudyVault's catalog: backend development notes, Java, Spring Boot, SQL, DSA, Python, system design, cloud, DevOps and interview preparation material.",
};

export default function ProductsPage() {
  return (
    <Suspense fallback={<div className="mx-auto max-w-7xl px-4 py-16 text-slate-400">Loading catalog…</div>}>
      <ProductsClient />
    </Suspense>
  );
}
