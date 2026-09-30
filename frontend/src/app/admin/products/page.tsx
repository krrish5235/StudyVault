"use client";

import { useCallback, useEffect, useState } from "react";
import { Plus, Upload, Archive, Send, Undo2, Loader2 } from "lucide-react";
import { api, ApiError } from "@/lib/api";
import { formatINR } from "@/lib/format";
import type { Category, Page } from "@/lib/types";

interface AdminProduct {
  id: string; title: string; slug: string; shortDescription: string;
  categoryId: string; categoryName: string; tags: string[];
  priceMinor: number; comparePriceMinor: number | null; currency: string;
  status: "DRAFT" | "PUBLISHED" | "ARCHIVED"; featured: boolean; bestseller: boolean;
  reviewCount: number; viewCount: number; contentVersion: number;
}

interface FileVersion {
  id: string; version: number; originalFilename: string | null; sizeBytes: number; current: boolean;
}

const EMPTY_FORM = {
  title: "", shortDescription: "", description: "", categoryId: "",
  priceRupees: "", comparePriceRupees: "", tags: "", featured: false, bestseller: false,
};

export default function AdminProductsPage() {
  const [products, setProducts] = useState<Page<AdminProduct> | null>(null);
  const [categories, setCategories] = useState<Category[]>([]);
  const [showForm, setShowForm] = useState(false);
  const [editing, setEditing] = useState<AdminProduct | null>(null);
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

  function startCreate() {
    setEditing(null);
    setForm({ ...EMPTY_FORM, categoryId: categories[0]?.id ?? "" });
    setShowForm(true);
    setError(null);
  }

  function startEdit(p: AdminProduct) {
    setEditing(p);
    setForm({
      title: p.title,
      shortDescription: p.shortDescription,
      description: "",
      categoryId: p.categoryId,
      priceRupees: String(p.priceMinor / 100),
      comparePriceRupees: p.comparePriceMinor != null ? String(p.comparePriceMinor / 100) : "",
      tags: p.tags.join(", "),
      featured: p.featured,
      bestseller: p.bestseller,
    });
    setShowForm(true);
    setError(null);
  }

  async function save() {
    setBusy(true);
    setError(null);
    try {
      const payload = {
        title: form.title,
        shortDescription: form.shortDescription,
        ...(form.description ? { description: form.description } : {}),
        categoryId: form.categoryId,
        priceMinor: Math.round(Number(form.priceRupees) * 100),
        comparePriceMinor: form.comparePriceRupees ? Math.round(Number(form.comparePriceRupees) * 100) : null,
        tags: form.tags.split(",").map((t) => t.trim()).filter(Boolean),
        featured: form.featured,
        bestseller: form.bestseller,
      };
      if (editing) {
        await api.patch(`/api/v1/admin/products/${editing.id}`, payload);
      } else {
        await api.post("/api/v1/admin/products", payload);
      }
      setShowForm(false);
      load();
    } catch (err) {
      setError(err instanceof ApiError ? `${err.message}${err.fieldErrors ? `: ${Object.values(err.fieldErrors).join(", ")}` : ""}` : "Save failed.");
    } finally {
      setBusy(false);
    }
  }

  async function action(path: string, method: "post" | "delete", message: string) {
    if (message && !window.confirm(message)) return;
    try {
      await api[method](path);
      load();
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Action failed.");
    }
  }

  async function uploadFile(productId: string, kind: "digital" | "cover", file: File) {
    const body = new FormData();
    body.append("file", file);
    try {
      const res = await fetch(`${process.env.NEXT_PUBLIC_API_URL ?? ""}/api/v1/admin/products/${productId}/files/${kind}`, {
        method: "POST",
        credentials: "include",
        headers: { "X-XSRF-TOKEN": document.cookie.match(/sv_csrf=([^;]+)/)?.[1] ?? "" },
        body,
      });
      if (!res.ok) throw new Error((await res.json()).message ?? "Upload failed");
      load();
    } catch (err) {
      setError(err instanceof Error ? err.message : "Upload failed.");
    }
  }

  return (
    <div>
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold text-white">Products</h1>
        <button onClick={startCreate} className="flex items-center gap-2 rounded-xl bg-accent-600 px-4 py-2 text-sm font-semibold text-white hover:bg-accent-500">
          <Plus className="h-4 w-4" aria-hidden /> New product
        </button>
      </div>

      {error && <p role="alert" className="mt-4 rounded-xl border border-red-500/30 bg-red-500/10 px-4 py-3 text-sm text-red-300">{error}</p>}

      {showForm && (
        <div className="glass mt-6 rounded-2xl p-6">
          <h2 className="font-semibold text-white">{editing ? `Edit: ${editing.title}` : "Create product"}</h2>
          <div className="mt-4 grid gap-4 md:grid-cols-2">
            <input placeholder="Title" value={form.title} onChange={(e) => setForm({ ...form, title: e.target.value })}
              className="rounded-xl border border-white/10 bg-ink-800/60 px-4 py-2.5 text-sm text-white" />
            <select value={form.categoryId} onChange={(e) => setForm({ ...form, categoryId: e.target.value })}
              className="rounded-xl border border-white/10 bg-ink-800/60 px-4 py-2.5 text-sm text-white">
              {categories.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
            </select>
            <input placeholder="Short description (10-300 chars)" value={form.shortDescription}
              onChange={(e) => setForm({ ...form, shortDescription: e.target.value })}
              className="rounded-xl border border-white/10 bg-ink-800/60 px-4 py-2.5 text-sm text-white md:col-span-2" />
            <textarea placeholder={editing ? "New description (leave empty to keep current)" : "Full description (min 20 chars)"} rows={4}
              value={form.description} onChange={(e) => setForm({ ...form, description: e.target.value })}
              className="rounded-xl border border-white/10 bg-ink-800/60 px-4 py-2.5 text-sm text-white md:col-span-2" />
            <input placeholder="Price in ₹" type="number" min="0" step="0.01" value={form.priceRupees}
              onChange={(e) => setForm({ ...form, priceRupees: e.target.value })}
              className="rounded-xl border border-white/10 bg-ink-800/60 px-4 py-2.5 text-sm text-white" />
            <input placeholder="Compare-at price in ₹ (optional)" type="number" min="0" step="0.01" value={form.comparePriceRupees}
              onChange={(e) => setForm({ ...form, comparePriceRupees: e.target.value })}
              className="rounded-xl border border-white/10 bg-ink-800/60 px-4 py-2.5 text-sm text-white" />
            <input placeholder="Tags (comma separated)" value={form.tags} onChange={(e) => setForm({ ...form, tags: e.target.value })}
              className="rounded-xl border border-white/10 bg-ink-800/60 px-4 py-2.5 text-sm text-white" />
            <div className="flex items-center gap-6 text-sm text-slate-300">
              <label className="flex items-center gap-2">
                <input type="checkbox" checked={form.featured} onChange={(e) => setForm({ ...form, featured: e.target.checked })} /> Featured
              </label>
              <label className="flex items-center gap-2">
                <input type="checkbox" checked={form.bestseller} onChange={(e) => setForm({ ...form, bestseller: e.target.checked })} /> Bestseller
              </label>
            </div>
          </div>
          <div className="mt-4 flex gap-3">
            <button onClick={() => void save()} disabled={busy}
              className="rounded-xl bg-accent-600 px-6 py-2.5 text-sm font-semibold text-white hover:bg-accent-500 disabled:opacity-50">
              {busy ? "Saving…" : editing ? "Save changes" : "Create draft"}
            </button>
            <button onClick={() => setShowForm(false)} className="rounded-xl border border-white/15 px-6 py-2.5 text-sm text-slate-300 hover:bg-white/5">
              Cancel
            </button>
          </div>
        </div>
      )}

      <div className="glass mt-6 overflow-x-auto rounded-2xl">
        <table className="w-full text-sm">
          <thead className="text-left text-xs uppercase text-slate-500">
            <tr>
              <th className="p-4">Product</th><th className="p-4">Price</th><th className="p-4">Status</th>
              <th className="p-4">Flags</th><th className="p-4">Files / actions</th>
            </tr>
          </thead>
          <tbody>
            {products?.content.map((p) => (
              <tr key={p.id} className="border-t border-white/5 align-top">
                <td className="p-4">
                  <p className="font-semibold text-white">{p.title}</p>
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
                <td className="p-4 text-xs text-slate-400">
                  {p.featured && "featured "}{p.bestseller && "bestseller"}
                </td>
                <td className="p-4">
                  <div className="flex flex-wrap gap-2">
                    {p.status !== "PUBLISHED" ? (
                      <button onClick={() => void action(`/api/v1/admin/products/${p.id}/publish`, "post", "")}
                        className="flex items-center gap-1 rounded-lg bg-mint-400/10 px-3 py-1.5 text-xs font-semibold text-mint-400 hover:bg-mint-400/20">
                        <Send className="h-3 w-3" aria-hidden /> Publish
                      </button>
                    ) : (
                      <button onClick={() => void action(`/api/v1/admin/products/${p.id}/unpublish`, "post", "")}
                        className="flex items-center gap-1 rounded-lg bg-white/5 px-3 py-1.5 text-xs text-slate-300 hover:bg-white/10">
                        <Undo2 className="h-3 w-3" aria-hidden /> Unpublish
                      </button>
                    )}
                    <button onClick={() => startEdit(p)} className="rounded-lg bg-white/5 px-3 py-1.5 text-xs text-slate-300 hover:bg-white/10">Edit</button>
                    <label className="flex cursor-pointer items-center gap-1 rounded-lg bg-white/5 px-3 py-1.5 text-xs text-slate-300 hover:bg-white/10">
                      <Upload className="h-3 w-3" aria-hidden /> File
                      <input type="file" accept=".pdf,.zip" className="hidden"
                        onChange={(e) => e.target.files?.[0] && void uploadFile(p.id, "digital", e.target.files[0])} />
                    </label>
                    <label className="flex cursor-pointer items-center gap-1 rounded-lg bg-white/5 px-3 py-1.5 text-xs text-slate-300 hover:bg-white/10">
                      <Upload className="h-3 w-3" aria-hidden /> Cover
                      <input type="file" accept="image/png,image/jpeg,image/webp" className="hidden"
                        onChange={(e) => e.target.files?.[0] && void uploadFile(p.id, "cover", e.target.files[0])} />
                    </label>
                    {p.status !== "ARCHIVED" && (
                      <button onClick={() => void action(`/api/v1/admin/products/${p.id}`, "delete", `Archive "${p.title}"?`)}
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
