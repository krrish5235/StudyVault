"use client";

import { useCallback, useEffect, useState } from "react";
import Link from "next/link";
import { useParams, useRouter } from "next/navigation";
import {
  ArrowLeft, Upload, Check, Send, Undo2, Archive, Loader2, FileArchive,
  ImageIcon, RotateCcw, ExternalLink, Trash2,
} from "lucide-react";
import { api, ApiError } from "@/lib/api";
import { formatINR } from "@/lib/format";
import type { Category } from "@/lib/types";

interface AdminProduct {
  id: string; title: string; slug: string; shortDescription: string; description?: string;
  categoryId: string; categoryName: string; tags: string[];
  priceMinor: number; comparePriceMinor: number | null; currency: string;
  status: "DRAFT" | "PUBLISHED" | "ARCHIVED"; featured: boolean; bestseller: boolean;
  reviewCount: number; viewCount: number; contentVersion: number;
  coverImageKey: string | null; coverUrl: string | null; updatedAt: string;
}

interface FileVersion {
  id: string; version: number; originalFilename: string | null;
  contentType: string; sizeBytes: number; current: boolean; uploadedAt: string;
}

interface PreviewImage {
  id: string; url: string | null; sortOrder: number;
}

export default function AdminProductPortal() {
  const params = useParams<{ id: string }>();
  const productId = params.id;
  const router = useRouter();

  const [product, setProduct] = useState<AdminProduct | null>(null);
  const [categories, setCategories] = useState<Category[]>([]);
  const [versions, setVersions] = useState<FileVersion[]>([]);
  const [previews, setPreviews] = useState<PreviewImage[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  const [form, setForm] = useState({
    title: "", shortDescription: "", description: "", categoryId: "",
    priceRupees: "", comparePriceRupees: "", tags: "", featured: false, bestseller: false,
  });

  const load = useCallback(async () => {
    try {
      const p = await api.get<AdminProduct>(`/api/v1/admin/products/${productId}`);
      setProduct(p);
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
      setVersions(await api.get<FileVersion[]>(`/api/v1/admin/products/${productId}/files`));
      setPreviews(await api.get<PreviewImage[]>(`/api/v1/admin/products/${productId}/files/previews`));
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Could not load the product.");
    }
  }, [productId]);

  useEffect(() => {
    void load();
    api.get<Category[]>("/api/v1/categories").then(setCategories).catch(() => {});
  }, [load]);

  function flash(message: string) {
    setNotice(message);
    setError(null);
    setTimeout(() => setNotice(null), 3000);
  }

  function fail(err: unknown, fallback: string) {
    setError(err instanceof ApiError ? err.message : fallback);
    setTimeout(() => setError(null), 5000);
  }

  async function save() {
    setBusy(true);
    try {
      await api.patch(`/api/v1/admin/products/${productId}`, {
        title: form.title,
        shortDescription: form.shortDescription,
        ...(form.description ? { description: form.description } : {}),
        categoryId: form.categoryId,
        priceMinor: Math.round(Number(form.priceRupees) * 100),
        comparePriceMinor: form.comparePriceRupees ? Math.round(Number(form.comparePriceRupees) * 100) : null,
        tags: form.tags.split(",").map((t) => t.trim()).filter(Boolean),
        featured: form.featured,
        bestseller: form.bestseller,
      });
      flash("Product details saved.");
      await load();
    } catch (err) {
      fail(err, "Save failed.");
    } finally {
      setBusy(false);
    }
  }

  async function setStatus(status: "PUBLISHED" | "DRAFT", message: string) {
    try {
      await api.post(`/api/v1/admin/products/${productId}/${status === "PUBLISHED" ? "publish" : "unpublish"}`);
      flash(message);
      await load();
    } catch (err) {
      fail(err, "Action failed.");
    }
  }

  async function archive() {
    if (!window.confirm("Archive this product? It will be hidden from the store.")) return;
    try {
      await api.delete(`/api/v1/admin/products/${productId}`);
      flash("Product archived.");
      await load();
    } catch (err) {
      fail(err, "Archive failed.");
    }
  }

  async function upload(kind: "digital" | "cover", file: File) {
    setBusy(true);
    try {
      await api.upload(`/api/v1/admin/products/${productId}/files/${kind}`, file);
      flash(kind === "digital" ? "Notes file uploaded (new version)." : "Cover image uploaded.");
      await load();
    } catch (err) {
      fail(err, "Upload failed.");
    } finally {
      setBusy(false);
    }
  }

  async function uploadPreview(file: File) {
    setBusy(true);
    try {
      await api.upload(`/api/v1/admin/products/${productId}/files/previews`, file);
      flash("Preview image uploaded.");
      await load();
    } catch (err) {
      fail(err, "Upload failed.");
    } finally {
      setBusy(false);
    }
  }

  async function activateVersion(fileId: string) {
    try {
      await api.post(`/api/v1/admin/products/${productId}/files/${fileId}/activate`);
      flash("Rollback complete - that version is now live for buyers.");
      await load();
    } catch (err) {
      fail(err, "Rollback failed.");
    }
  }

  async function deletePreview(previewId: string) {
    try {
      await api.raw(`/api/v1/admin/products/${productId}/files/previews/${previewId}`, "DELETE");
      await load();
    } catch (err) {
      fail(err, "Delete failed.");
    }
  }

  if (!product) {
    return <div className="flex items-center gap-2 py-20 text-slate-400"><Loader2 className="h-5 w-5 animate-spin" aria-hidden /> Loading product…</div>;
  }

  return (
    <div>
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <Link href="/admin/products" className="flex items-center gap-1 text-sm text-slate-400 hover:text-white">
            <ArrowLeft className="h-4 w-4" aria-hidden /> All products
          </Link>
          <h1 className="mt-1 text-2xl font-bold text-white">{product.title}</h1>
          <p className="text-xs text-slate-500">
            /{product.slug} · v{product.contentVersion} · {product.viewCount} views · {product.reviewCount} reviews
          </p>
        </div>
        <div className="flex flex-wrap items-center gap-2">
          <span className={`rounded-full px-3 py-1 text-xs font-bold ${
            product.status === "PUBLISHED" ? "bg-mint-400/10 text-mint-400"
            : product.status === "DRAFT" ? "bg-gold-500/10 text-gold-400" : "bg-white/5 text-slate-400"}`}>
            {product.status}
          </span>
          {product.status !== "PUBLISHED" ? (
            <button onClick={() => void setStatus("PUBLISHED", "Product is now live in the store.")}
              className="flex items-center gap-1.5 rounded-xl bg-mint-400/10 px-4 py-2 text-sm font-semibold text-mint-400 hover:bg-mint-400/20">
              <Send className="h-4 w-4" aria-hidden /> Publish
            </button>
          ) : (
            <button onClick={() => void setStatus("DRAFT", "Moved back to draft.")}
              className="flex items-center gap-1.5 rounded-xl bg-white/5 px-4 py-2 text-sm text-slate-300 hover:bg-white/10">
              <Undo2 className="h-4 w-4" aria-hidden /> Unpublish
            </button>
          )}
          <Link href={`/products/${product.slug}`} className="flex items-center gap-1.5 rounded-xl border border-white/15 px-4 py-2 text-sm text-slate-200 hover:bg-white/5">
            <ExternalLink className="h-4 w-4" aria-hidden /> View in store
          </Link>
          {product.status !== "ARCHIVED" && (
            <button onClick={() => void archive()}
              className="flex items-center gap-1.5 rounded-xl bg-red-500/10 px-4 py-2 text-sm text-red-300 hover:bg-red-500/20">
              <Archive className="h-4 w-4" aria-hidden /> Archive
            </button>
          )}
        </div>
      </div>

      {notice && <p className="mt-4 rounded-xl border border-mint-400/30 bg-mint-400/10 px-4 py-3 text-sm text-mint-400">{notice}</p>}
      {error && <p role="alert" className="mt-4 rounded-xl border border-red-500/30 bg-red-500/10 px-4 py-3 text-sm text-red-300">{error}</p>}

      <div className="mt-6 grid gap-6 lg:grid-cols-2">
        {/* ---------------- Cover ---------------- */}
        <section className="glass rounded-2xl p-6">
          <h2 className="flex items-center gap-2 font-semibold text-white">
            <ImageIcon className="h-4 w-4 text-accent-400" aria-hidden /> Cover page
          </h2>
          <div className="mt-4 flex gap-5">
            <div className="h-44 w-32 shrink-0 overflow-hidden rounded-xl border border-white/10 bg-gradient-to-br from-ink-700 to-ink-900">
              {product.coverUrl ? (
                // eslint-disable-next-line @next/next/no-img-element
                <img src={`${process.env.NEXT_PUBLIC_API_URL ?? ""}${product.coverUrl}`} alt="Current cover" className="h-full w-full object-cover" />
              ) : (
                <div className="flex h-full items-center justify-center text-3xl font-black text-white/10">?</div>
              )}
            </div>
            <div className="flex-1 text-sm text-slate-400">
              <p>This is what buyers see on the catalog card and product page.</p>
              <label className="mt-4 flex w-fit cursor-pointer items-center gap-2 rounded-xl bg-accent-600 px-4 py-2.5 font-semibold text-white hover:bg-accent-500">
                <Upload className="h-4 w-4" aria-hidden /> {product.coverUrl ? "Replace cover" : "Upload cover"}
                <input type="file" accept="image/png,image/jpeg,image/webp" className="hidden"
                  onChange={(e) => e.target.files?.[0] && void upload("cover", e.target.files[0])} />
              </label>
              <p className="mt-2 text-xs text-slate-500">PNG / JPG / WebP · up to 5 MB</p>
            </div>
          </div>

          {/* ---------------- Previews ---------------- */}
          <h3 className="mt-6 text-sm font-semibold text-slate-200">Preview images (shown on the product page)</h3>
          <div className="mt-3 flex flex-wrap gap-3">
            {previews.map((p) => (
              <div key={p.id} className="group relative h-24 w-20 overflow-hidden rounded-lg border border-white/10">
                {p.url && (
                  // eslint-disable-next-line @next/next/no-img-element
                  <img src={`${process.env.NEXT_PUBLIC_API_URL ?? ""}${p.url}`} alt={`Preview ${p.sortOrder + 1}`} className="h-full w-full object-cover" />
                )}
                <button onClick={() => void deletePreview(p.id)} aria-label="Delete preview"
                  className="absolute right-1 top-1 rounded bg-ink-900/80 p-1 text-red-300 opacity-0 transition group-hover:opacity-100">
                  <Trash2 className="h-3 w-3" aria-hidden />
                </button>
              </div>
            ))}
            {previews.length < 6 && (
              <label className="flex h-24 w-20 cursor-pointer flex-col items-center justify-center gap-1 rounded-lg border border-dashed border-white/20 text-xs text-slate-500 hover:border-accent-400 hover:text-slate-300">
                <Upload className="h-4 w-4" aria-hidden /> Add
                <input type="file" accept="image/png,image/jpeg,image/webp" className="hidden"
                  onChange={(e) => e.target.files?.[0] && void uploadPreview(e.target.files[0])} />
              </label>
            )}
          </div>
        </section>

        {/* ---------------- Digital file ---------------- */}
        <section className="glass rounded-2xl p-6">
          <h2 className="flex items-center gap-2 font-semibold text-white">
            <FileArchive className="h-4 w-4 text-accent-400" aria-hidden /> Notes file (what buyers download)
          </h2>
          {versions.length === 0 ? (
            <p className="mt-3 rounded-xl border border-gold-500/30 bg-gold-500/10 px-4 py-3 text-sm text-gold-400">
              No file uploaded yet — buyers can't download this product until you upload one.
            </p>
          ) : (
            <ul className="mt-4 space-y-2">
              {versions.map((v) => (
                <li key={v.id} className={`flex items-center gap-3 rounded-xl border px-4 py-3 text-sm ${
                  v.current ? "border-mint-400/30 bg-mint-400/5" : "border-white/5"}`}>
                  <span className="font-mono text-xs text-slate-400">v{v.version}</span>
                  <span className="flex-1 truncate text-slate-200">{v.originalFilename ?? "file"}</span>
                  <span className="text-xs text-slate-500">{(v.sizeBytes / 1024 / 1024).toFixed(2)} MB</span>
                  {v.current ? (
                    <span className="flex items-center gap-1 rounded-full bg-mint-400/10 px-2.5 py-0.5 text-xs font-bold text-mint-400">
                      <Check className="h-3 w-3" aria-hidden /> live
                    </span>
                  ) : (
                    <button onClick={() => void activateVersion(v.id)}
                      className="flex items-center gap-1 rounded-lg bg-white/5 px-2.5 py-1 text-xs text-slate-300 hover:bg-white/10">
                      <RotateCcw className="h-3 w-3" aria-hidden /> Make live
                    </button>
                  )}
                </li>
              ))}
            </ul>
          )}
          <label className="mt-4 flex w-fit cursor-pointer items-center gap-2 rounded-xl bg-accent-600 px-4 py-2.5 font-semibold text-white hover:bg-accent-500">
            {busy ? <Loader2 className="h-4 w-4 animate-spin" aria-hidden /> : <Upload className="h-4 w-4" aria-hidden />}
            {versions.length === 0 ? "Upload notes (PDF/ZIP)" : "Upload new version"}
            <input type="file" accept=".pdf,.zip" className="hidden"
              onChange={(e) => e.target.files?.[0] && void upload("digital", e.target.files[0])} />
          </label>
          <p className="mt-2 text-xs text-slate-500">PDF or ZIP · up to 200 MB · old versions are kept and can be restored anytime</p>
        </section>
      </div>

      {/* ---------------- Details ---------------- */}
      <section className="glass mt-6 rounded-2xl p-6">
        <h2 className="font-semibold text-white">Product details</h2>
        <div className="mt-4 grid gap-4 md:grid-cols-2">
          <input value={form.title} onChange={(e) => setForm({ ...form, title: e.target.value })}
            placeholder="Title" aria-label="Title"
            className="rounded-xl border border-white/10 bg-ink-800/60 px-4 py-2.5 text-sm text-white" />
          <select value={form.categoryId} onChange={(e) => setForm({ ...form, categoryId: e.target.value })}
            aria-label="Category"
            className="rounded-xl border border-white/10 bg-ink-800/60 px-4 py-2.5 text-sm text-white">
            {categories.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
          </select>
          <input value={form.shortDescription} onChange={(e) => setForm({ ...form, shortDescription: e.target.value })}
            placeholder="Short description" aria-label="Short description"
            className="rounded-xl border border-white/10 bg-ink-800/60 px-4 py-2.5 text-sm text-white md:col-span-2" />
          <textarea value={form.description} rows={4} onChange={(e) => setForm({ ...form, description: e.target.value })}
            placeholder="Full description (leave empty to keep the current one)" aria-label="Full description"
            className="rounded-xl border border-white/10 bg-ink-800/60 px-4 py-2.5 text-sm text-white md:col-span-2" />
          <input value={form.priceRupees} type="number" min="0" step="0.01" onChange={(e) => setForm({ ...form, priceRupees: e.target.value })}
            placeholder="Price ₹" aria-label="Price in rupees"
            className="rounded-xl border border-white/10 bg-ink-800/60 px-4 py-2.5 text-sm text-white" />
          <input value={form.comparePriceRupees} type="number" min="0" step="0.01" onChange={(e) => setForm({ ...form, comparePriceRupees: e.target.value })}
            placeholder="Compare-at price ₹ (optional)" aria-label="Compare price in rupees"
            className="rounded-xl border border-white/10 bg-ink-800/60 px-4 py-2.5 text-sm text-white" />
          <input value={form.tags} onChange={(e) => setForm({ ...form, tags: e.target.value })}
            placeholder="Tags (comma separated)" aria-label="Tags"
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
        <button onClick={() => void save()} disabled={busy}
          className="mt-5 rounded-xl bg-accent-600 px-6 py-2.5 font-semibold text-white hover:bg-accent-500 disabled:opacity-50">
          {busy ? "Saving…" : "Save details"}
        </button>
      </section>
    </div>
  );
}
