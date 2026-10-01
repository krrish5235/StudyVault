"use client";

import { useCallback, useEffect, useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { Plus, Archive, Send, Undo2, Settings2 } from "lucide-react";
import { api, ApiError } from "@/lib/api";
import { formatINR } from "@/lib/format";
import type { Category, Page } from "@/lib/types";

interface AdminProduct {
  id: string; title: string; slug: string; shortDescription: string;
  categoryId: string; categoryName: string; tags: string[];
  priceMinor: number; comparePriceMinor: number | null; currency: string;
  status: "DRAFT" | "PUBLISHED" | "ARCHIVED"; featured: boolean; bestseller: boolean;
  reviewCount: number; viewCount: number; contentVersion: number;
  coverImageKey: string | null; coverUrl: string | null;
}

const EMPTY_FORM = {
  title: "", shortDescription: "", description: "", categoryId: "",
  priceRupees: "", comparePriceRupees: "", tags: "", featured: false, bestseller: false,
};

export default function AdminProductsPage() {
  const router = useRouter();
  const [products, setProducts] = useState<Page<AdminProduct> | null>(null);
  const [categories, setCategories] = useState<Category[]>([]);
  const [showForm, setShowForm] = useState(false);
  const [form, setForm] = useState(EMPTY_FORM);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  const load = useCallback(() => {
    api.get<Page<AdminProduct>>("/api/v1/admin/products?size=50").then(setProducts).catch(() => setProducts(null));
  }, []);

  useEffect(() => {
    load();
    api.get<Category[]>("/api/v1/categories").then(setCategories).catch(() => {});
  }, [load]);

  async function create() {
    setBusy(true);
    setError(null);
    try {
      const created = await api.post<AdminProduct>("/api/v1/admin/products", {
        title: form.title,
        shortDescription: form.shortDescription,
        description: form.description,
        categoryId: form.categoryId,
        priceMinor: Math.round(Number(form.priceRupees) * 100),
        comparePriceMinor: form.comparePriceRupees ? Math.round(Number(form.comparePriceRupees) * 100) : null,
        tags: form.tags.split(",").map((t) => t.trim()).filter(Boolean),
        featured: form.featured,
        bestseller: form.bestseller,
      });
      setShowForm(false);
      setForm(EMPTY_FORM);
      // Straight into the product portal to upload the cover and notes file.
      router.push(`/admin/products/${created.id}`);
    } catch (err) {
      setError(err instanceof ApiError ? `${err.message}${err.fieldErrors ? `: ${Object.values(err.fieldErrors).join(", ")}` : ""}` : "Save failed.");
    } finally {
      setBusy(false);
    }
  }

  async function quickAction(path: string, method: "post" | "delete", message: string) {
    if (message && !window.confirm(message)) return;
    try {
      await api[method](path);
      load();
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Action failed.");
      setTimeout(() => setError(null), 5000);
    }
  }

  return (
    <div>
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold text-white">Products</h1>
        <button onClick={() => setShowForm(!showForm)}
          className="flex items-center gap-2 rounded-xl bg-accent-600 px-4 py-2 text-sm font-semibold text-white hover:bg-accent-500">
          <Plus className="h-4 w-4" aria-hidden /> New product
        </button>
      </div>

      {error && <p role="alert" className="mt-4 rounded-xl border border-red-500/30 bg-red-500/10 px-4 py-3 text-sm text-red-300">{error}</p>}

      {showForm && (
        <div className="glass mt-6 rounded-2xl p-6">
          <h2 className="font-semibold text-white">Create product</h2>
          <p className="mt-1 text-xs text-slate-500">Creates a draft — you'll upload the cover and notes file on the next screen.</p>
          <div className="mt-4 grid gap-4 md:grid-cols-2">
            <input placeholder="Title" value={form.title} onChange={(e) => setForm({ ...form, title: e.target.value })}
              className="rounded-xl border border-white/10 bg-ink-800/60 px-4 py-2.5 text-sm text-white" />
            <select value={form.categoryId} onChange={(e) => setForm({ ...form, categoryId: e.target.value })}
              aria-label="Category" className="rounded-xl border border-white/10 bg-ink-800/60 px-4 py-2.5 text-sm text-white">
              <option value="">Select category…</option>
              {categories.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
            </select>
            <input placeholder="Short description (10-300 chars)" value={form.shortDescription}
              onChange={(e) => setForm({ ...form, shortDescription: e.target.value })}
              className="rounded-xl border border-white/10 bg-ink-800/60 px-4 py-2.5 text-sm text-white md:col-span-2" />
            <textarea placeholder="Full description (min 20 chars)" rows={4} value={form.description}
              onChange={(e) => setForm({ ...form, description: e.target.value })}
              className="rounded-xl border border-white/10 bg-ink-800/60 px-4 py-2.5 text-sm text-white md:col-span-2" />
            <input placeholder="Price in ₹" type="number" min="0" step="0.01" value={form.priceRupees}
              onChange={(e) => setForm({ ...form, priceRupees: e.target.value })}
              className="rounded-xl border border-white/10 bg-ink-800/60 px-4 py-2.5 text-sm text-white" />
            <input placeholder="Compare-at price ₹ (optional)" type="number" min="0" step="0.01" value={form.comparePriceRupees}
              onChange={(e) => setForm({ ...form, comparePriceRupees: e.target.value })}
              className="rounded-xl border border-white/10 bg-ink-800/60 px-4 py-2.5 text-sm text-white" />
            <input placeholder="Tags (comma separated)" value={form.tags} onChange={(e) => setForm({ ...form, tags: e.target.value })}
              className="rounded-xl border border-white/10 bg-ink-800/60 px-4 py-2.5 text-sm text-white" />
          </div>
          <button onClick={() => void create()} disabled={busy}
            className="mt-4 rounded-xl bg-accent-600 px-6 py-2.5 text-sm font-semibold text-white hover:bg-accent-500 disabled:opacity-50">
            {busy ? "Creating…" : "Create and add content →"}
          </button>
        </div>
      )}

      <div className="glass mt-6 overflow-hidden rounded-2xl">
        <table className="w-full text-sm">
          <thead className="text-left text-xs uppercase text-slate-500">
            <tr>
              <th className="p-4">Product</th><th className="p-4">Price</th><th className="p-4">Status</th>
              <th className="p-4">Cover</th><th className="p-4">Actions</th>
            </tr>
          </thead>
          <tbody>
            {products?.content.map((p) => (
              <tr key={p.id} className="border-t border-white/5 align-middle">
                <td className="p-4">
                  <Link href={`/admin/products/${p.id}`} className="font-semibold text-white hover:text-accent-400">{p.title}</Link>
                  <p className="text-xs text-slate-500">{p.categoryName} · v{p.contentVersion} · {p.viewCount} views</p>
                </td>
                <td className="p-4 text-white">{formatINR(p.priceMinor)}</td>
                <td className="p-4">
                  <span className={`rounded-full px-2.5 py-0.5 text-xs font-bold ${
                    p.status === "PUBLISHED" ? "bg-mint-400/10 text-mint-400"
                    : p.status === "DRAFT" ? "bg-gold-500/10 text-gold-400" : "bg-white/5 text-slate-400"}`}>
                    {p.status}
                  </span>
                </td>
                <td className="p-4">
                  {p.coverUrl ? (
                    // eslint-disable-next-line @next/next/no-img-element
                    <img src={`${process.env.NEXT_PUBLIC_API_URL ?? ""}${p.coverUrl}`} alt="" className="h-12 w-9 rounded object-cover" />
                  ) : (
                    <span className="text-xs text-slate-600">no cover</span>
                  )}
                </td>
                <td className="p-4">
                  <div className="flex flex-wrap gap-2">
                    <Link href={`/admin/products/${p.id}`}
                      className="flex items-center gap-1 rounded-lg bg-accent-600/15 px-3 py-1.5 text-xs font-semibold text-accent-400 hover:bg-accent-600/25">
                      <Settings2 className="h-3 w-3" aria-hidden /> Manage content
                    </Link>
                    {p.status !== "PUBLISHED" ? (
                      <button onClick={() => void quickAction(`/api/v1/admin/products/${p.id}/publish`, "post", "")}
                        className="flex items-center gap-1 rounded-lg bg-mint-400/10 px-3 py-1.5 text-xs font-semibold text-mint-400 hover:bg-mint-400/20">
                        <Send className="h-3 w-3" aria-hidden /> Publish
                      </button>
                    ) : (
                      <button onClick={() => void quickAction(`/api/v1/admin/products/${p.id}/unpublish`, "post", "")}
                        className="flex items-center gap-1 rounded-lg bg-white/5 px-3 py-1.5 text-xs text-slate-300 hover:bg-white/10">
                        <Undo2 className="h-3 w-3" aria-hidden /> Unpublish
                      </button>
                    )}
                    {p.status !== "ARCHIVED" && (
                      <button onClick={() => void quickAction(`/api/v1/admin/products/${p.id}`, "delete", `Archive "${p.title}"?`)}
                        className="flex items-center gap-1 rounded-lg bg-red-500/10 px-3 py-1.5 text-xs text-red-300 hover:bg-red-500/20">
                        <Archive className="h-3 w-3" aria-hidden /> Archive
                      </button>
                    )}
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
        {products?.content.length === 0 && <p className="p-6 text-sm text-slate-500">No products yet — create the first one.</p>}
      </div>
    </div>
  );
}
