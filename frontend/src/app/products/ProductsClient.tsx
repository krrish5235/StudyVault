"use client";

import { useCallback, useEffect, useMemo, useState } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { Search, SlidersHorizontal } from "lucide-react";
import ProductCard from "@/components/product/ProductCard";
import { api } from "@/lib/api";
import type { Category, Page, ProductSummary } from "@/lib/types";

const SORTS = [
  { value: "newest", label: "Newest" },
  { value: "popular", label: "Most popular" },
  { value: "rating", label: "Highest rated" },
  { value: "price_asc", label: "Price: low → high" },
  { value: "price_desc", label: "Price: high → low" },
];

export default function ProductsClient() {
  const params = useSearchParams();
  const router = useRouter();

  const [query, setQuery] = useState(params.get("query") ?? "");
  const [category, setCategory] = useState(params.get("category") ?? "");
  const [sort, setSort] = useState(params.get("sort") ?? "newest");
  const [page, setPage] = useState(Number(params.get("page") ?? "0"));
  const [categories, setCategories] = useState<Category[]>([]);
  const [data, setData] = useState<Page<ProductSummary> | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    api.get<Category[]>("/api/v1/categories").then(setCategories).catch(() => setCategories([]));
  }, []);

  const searchParams = useMemo(() => {
    const sp = new URLSearchParams();
    if (query.trim()) sp.set("query", query.trim());
    if (category) sp.set("category", category);
    if (sort) sp.set("sort", sort);
    sp.set("page", String(page));
    sp.set("size", "12");
    return sp;
  }, [query, category, sort, page]);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const result = await api.get<Page<ProductSummary>>(`/api/v1/products?${searchParams.toString()}`);
      setData(result);
    } catch {
      setData(null);
    } finally {
      setLoading(false);
    }
  }, [searchParams]);

  useEffect(() => {
    void load();
    router.replace(`/products?${searchParams.toString()}`, { scroll: false });
  }, [load, router, searchParams]);

  const totalPages = data?.totalPages ?? 0;

  return (
    <div className="mx-auto max-w-7xl px-4 py-10">
      <h1 className="text-3xl font-bold text-white">Catalog</h1>
      <p className="mt-1 text-slate-400">
        {data ? `${data.totalElements} product${data.totalElements === 1 ? "" : "s"}` : "Loading…"}
      </p>

      <div className="mt-6 flex flex-col gap-4 md:flex-row md:items-center">
        <div className="relative flex-1">
          <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-500" aria-hidden />
          <input
            type="search"
            value={query}
            onChange={(e) => {
              setQuery(e.target.value);
              setPage(0);
            }}
            placeholder="Search notes, e-books, cheat sheets…"
            aria-label="Search products"
            className="w-full rounded-xl border border-white/10 bg-ink-800/60 py-2.5 pl-10 pr-4 text-sm text-white placeholder:text-slate-500 focus:border-accent-400"
          />
        </div>
        <div className="flex items-center gap-2">
          <SlidersHorizontal className="h-4 w-4 text-slate-500" aria-hidden />
          <select
            value={category}
            onChange={(e) => {
              setCategory(e.target.value);
              setPage(0);
            }}
            aria-label="Filter by category"
            className="rounded-xl border border-white/10 bg-ink-800/60 px-3 py-2.5 text-sm text-slate-200"
          >
            <option value="">All categories</option>
            {categories.map((c) => (
              <option key={c.id} value={c.slug}>{c.name}</option>
            ))}
          </select>
          <select
            value={sort}
            onChange={(e) => setSort(e.target.value)}
            aria-label="Sort products"
            className="rounded-xl border border-white/10 bg-ink-800/60 px-3 py-2.5 text-sm text-slate-200"
          >
            {SORTS.map((s) => (
              <option key={s.value} value={s.value}>{s.label}</option>
            ))}
          </select>
        </div>
      </div>

      {loading ? (
        <div className="mt-10 grid gap-5 sm:grid-cols-2 lg:grid-cols-4" aria-busy="true">
          {Array.from({ length: 8 }).map((_, i) => (
            <div key={i} className="glass h-72 animate-pulse rounded-2xl" />
          ))}
        </div>
      ) : data && data.content.length > 0 ? (
        <div className="mt-8 grid gap-5 sm:grid-cols-2 lg:grid-cols-4">
          {data.content.map((p) => (
            <ProductCard key={p.id} product={p} />
          ))}
        </div>
      ) : (
        <div className="py-24 text-center">
          <h2 className="text-xl font-semibold text-white">No products found</h2>
          <p className="mt-2 text-slate-400">Try a different search or category.</p>
        </div>
      )}

      {totalPages > 1 && (
        <nav className="mt-10 flex items-center justify-center gap-2" aria-label="Pagination">
          <button
            disabled={page === 0}
            onClick={() => setPage((p) => Math.max(0, p - 1))}
            className="rounded-lg border border-white/10 px-4 py-2 text-sm disabled:opacity-40"
          >
            Previous
          </button>
          <span className="px-3 text-sm text-slate-400">
            Page {page + 1} of {totalPages}
          </span>
          <button
            disabled={page >= totalPages - 1}
            onClick={() => setPage((p) => p + 1)}
            className="rounded-lg border border-white/10 px-4 py-2 text-sm disabled:opacity-40"
          >
            Next
          </button>
        </nav>
      )}
    </div>
  );
}
