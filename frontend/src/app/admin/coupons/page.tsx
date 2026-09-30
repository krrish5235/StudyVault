"use client";

import { useCallback, useEffect, useState } from "react";
import { api } from "@/lib/api";
import { formatINR } from "@/lib/format";
import type { Page as PageType } from "@/lib/types";

interface Coupon {
  id: string; code: string; discountType: "PERCENT" | "FIXED"; percentOff: number | null;
  amountOffMinor: number | null; usageLimit: number | null; usedCount: number;
  perUserLimit: number; active: boolean; expiresAt: string | null;
}

export default function AdminCouponsPage() {
  const [coupons, setCoupons] = useState<Coupon[] | null>(null);
  const [form, setForm] = useState({ code: "", discountType: "PERCENT", percentOff: "20", amountOffRupees: "", usageLimit: "100" });
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(() => {
    api.get<PageType<Coupon>>("/api/v1/admin/coupons?size=50").then((p) => setCoupons(p.content)).catch(() => setCoupons([]));
  }, []);

  useEffect(load, [load]);

  async function create() {
    setError(null);
    try {
      await fetch(`${process.env.NEXT_PUBLIC_API_URL ?? ""}/api/v1/admin/coupons`, {
        method: "POST", credentials: "include",
        headers: {
          "Content-Type": "application/json",
          "X-XSRF-TOKEN": document.cookie.match(/sv_csrf=([^;]+)/)?.[1] ?? "",
        },
        body: JSON.stringify({
          code: form.code,
          discountType: form.discountType,
          percentOff: form.discountType === "PERCENT" ? Number(form.percentOff) : null,
          amountOffMinor: form.discountType === "FIXED" ? Math.round(Number(form.amountOffRupees) * 100) : null,
          usageLimit: Number(form.usageLimit),
        }),
      }).then(async (r) => {
        if (!r.ok) throw new Error((await r.json()).message ?? "Create failed");
      });
      setForm({ ...form, code: "" });
      load();
    } catch (err) {
      setError(err instanceof Error ? err.message : "Create failed.");
    }
  }

  async function toggle(c: Coupon) {
    await fetch(`${process.env.NEXT_PUBLIC_API_URL ?? ""}/api/v1/admin/coupons/${c.id}/active`, {
      method: "PATCH", credentials: "include",
      headers: { "Content-Type": "application/json", "X-XSRF-TOKEN": document.cookie.match(/sv_csrf=([^;]+)/)?.[1] ?? "" },
      body: JSON.stringify({ active: !c.active }),
    });
    load();
  }

  return (
    <div>
      <h1 className="text-2xl font-bold text-white">Coupons</h1>

      <div className="glass mt-6 flex flex-wrap items-end gap-3 rounded-2xl p-5">
        <div>
          <label className="block text-xs text-slate-400">Code</label>
          <input value={form.code} onChange={(e) => setForm({ ...form, code: e.target.value.toUpperCase() })}
            placeholder="LAUNCH20" className="mt-1 w-36 rounded-xl border border-white/10 bg-ink-800/60 px-3 py-2 text-sm text-white" />
        </div>
        <div>
          <label className="block text-xs text-slate-400">Type</label>
          <select value={form.discountType} onChange={(e) => setForm({ ...form, discountType: e.target.value })}
            className="mt-1 rounded-xl border border-white/10 bg-ink-800/60 px-3 py-2 text-sm text-white">
            <option value="PERCENT">Percent</option>
            <option value="FIXED">Fixed ₹</option>
          </select>
        </div>
        {form.discountType === "PERCENT" ? (
          <div>
            <label className="block text-xs text-slate-400">% off</label>
            <input type="number" min="1" max="100" value={form.percentOff}
              onChange={(e) => setForm({ ...form, percentOff: e.target.value })}
              className="mt-1 w-20 rounded-xl border border-white/10 bg-ink-800/60 px-3 py-2 text-sm text-white" />
          </div>
        ) : (
          <div>
            <label className="block text-xs text-slate-400">₹ off</label>
            <input type="number" min="1" value={form.amountOffRupees}
              onChange={(e) => setForm({ ...form, amountOffRupees: e.target.value })}
              className="mt-1 w-24 rounded-xl border border-white/10 bg-ink-800/60 px-3 py-2 text-sm text-white" />
          </div>
        )}
        <div>
          <label className="block text-xs text-slate-400">Usage limit</label>
          <input type="number" min="1" value={form.usageLimit}
            onChange={(e) => setForm({ ...form, usageLimit: e.target.value })}
            className="mt-1 w-24 rounded-xl border border-white/10 bg-ink-800/60 px-3 py-2 text-sm text-white" />
        </div>
        <button onClick={() => void create()} className="rounded-xl bg-accent-600 px-5 py-2.5 text-sm font-semibold text-white hover:bg-accent-500">
          Create coupon
        </button>
      </div>
      {error && <p role="alert" className="mt-3 rounded-xl border border-red-500/30 bg-red-500/10 px-4 py-3 text-sm text-red-300">{error}</p>}

      <div className="glass mt-6 overflow-x-auto rounded-2xl">
        <table className="w-full text-sm">
          <thead className="text-left text-xs uppercase text-slate-500">
            <tr><th className="p-4">Code</th><th className="p-4">Discount</th><th className="p-4">Used</th><th className="p-4">State</th><th className="p-4"></th></tr>
          </thead>
          <tbody>
            {coupons?.map((c) => (
              <tr key={c.id} className="border-t border-white/5">
                <td className="p-4 font-mono font-semibold text-white">{c.code}</td>
                <td className="p-4 text-slate-300">
                  {c.discountType === "PERCENT" ? `${c.percentOff}%` : formatINR(c.amountOffMinor ?? 0)}
                </td>
                <td className="p-4 text-slate-400">{c.usedCount}{c.usageLimit != null ? ` / ${c.usageLimit}` : ""}</td>
                <td className="p-4">
                  <span className={`rounded-full px-2.5 py-0.5 text-xs font-bold ${c.active ? "bg-mint-400/10 text-mint-400" : "bg-white/5 text-slate-500"}`}>
                    {c.active ? "active" : "inactive"}
                  </span>
                </td>
                <td className="p-4">
                  <button onClick={() => void toggle(c)} className="rounded-lg bg-white/5 px-3 py-1.5 text-xs text-slate-300 hover:bg-white/10">
                    {c.active ? "Deactivate" : "Activate"}
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
        {coupons?.length === 0 && <p className="p-6 text-sm text-slate-500">No coupons yet.</p>}
      </div>
    </div>
  );
}
