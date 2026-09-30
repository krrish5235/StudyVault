"use client";

import { useCallback, useEffect, useState } from "react";
import { api } from "@/lib/api";
import { formatINR } from "@/lib/format";
import type { Page } from "@/lib/types";

interface AdminOrder {
  orderNumber: string; userEmail: string; status: string;
  totalMinor: number; paymentStatus: string; createdAt: string;
}

const STATUSES = ["", "PENDING", "PAID", "FAILED", "REFUNDED"];

export default function AdminOrdersPage() {
  const [orders, setOrders] = useState<Page<AdminOrder> | null>(null);
  const [status, setStatus] = useState("");

  const load = useCallback(() => {
    const q = status ? `&status=${status}` : "";
    api.get<Page<AdminOrder>>(`/api/v1/admin/orders?size=50${q}`).then(setOrders).catch(() => setOrders(null));
  }, [status]);

  useEffect(load, [load]);

  async function refund(orderNumber: string) {
    if (!window.confirm(`Record refund for ${orderNumber}? Access will be revoked.`)) return;
    try {
      await fetch(`${process.env.NEXT_PUBLIC_API_URL ?? ""}/api/v1/admin/orders/${orderNumber}/refund`, {
        method: "POST", credentials: "include",
        headers: { "X-XSRF-TOKEN": document.cookie.match(/sv_csrf=([^;]+)/)?.[1] ?? "" },
      });
      load();
    } catch {
      /* surfaced on next load */
    }
  }

  return (
    <div>
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold text-white">Orders</h1>
        <select value={status} onChange={(e) => setStatus(e.target.value)}
          className="rounded-xl border border-white/10 bg-ink-800/60 px-3 py-2 text-sm text-slate-200" aria-label="Filter by status">
          {STATUSES.map((s) => <option key={s} value={s}>{s || "All statuses"}</option>)}
        </select>
      </div>

      <div className="glass mt-6 overflow-x-auto rounded-2xl">
        <table className="w-full text-sm">
          <thead className="text-left text-xs uppercase text-slate-500">
            <tr><th className="p-4">Order</th><th className="p-4">Customer</th><th className="p-4">Status</th><th className="p-4">Payment</th><th className="p-4">Total</th><th className="p-4"></th></tr>
          </thead>
          <tbody>
            {orders?.content.map((o) => (
              <tr key={o.orderNumber} className="border-t border-white/5">
                <td className="p-4 font-mono text-xs text-slate-300">{o.orderNumber}</td>
                <td className="p-4 text-slate-300">{o.userEmail}</td>
                <td className="p-4"><span className="rounded-full bg-white/5 px-2.5 py-0.5 text-xs font-bold text-slate-200">{o.status}</span></td>
                <td className="p-4 text-xs text-slate-400">{o.paymentStatus}</td>
                <td className="p-4 text-white">{formatINR(o.totalMinor)}</td>
                <td className="p-4">
                  {o.status === "PAID" && (
                    <button onClick={() => void refund(o.orderNumber)}
                      className="rounded-lg bg-red-500/10 px-3 py-1.5 text-xs text-red-300 hover:bg-red-500/20">
                      Refund
                    </button>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
        {orders?.content.length === 0 && <p className="p-6 text-sm text-slate-500">No orders.</p>}
      </div>
    </div>
  );
}
