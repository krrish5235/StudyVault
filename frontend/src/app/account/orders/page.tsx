"use client";

import { useEffect, useState } from "react";
import { Package } from "lucide-react";
import { api } from "@/lib/api";
import { formatDate, formatINR } from "@/lib/format";

interface Order {
  orderNumber: string;
  status: string;
  subtotalMinor: number;
  discountMinor: number;
  totalMinor: number;
  currency: string;
  createdAt: string;
  items: { title: string; priceMinor: number }[];
}

const STATUS_STYLES: Record<string, string> = {
  PAID: "text-mint-400 bg-mint-400/10",
  PENDING: "text-gold-400 bg-gold-500/10",
  FAILED: "text-red-400 bg-red-500/10",
  REFUNDED: "text-slate-400 bg-white/5",
};

export default function OrdersPage() {
  const [orders, setOrders] = useState<Order[] | null>(null);

  useEffect(() => {
    api.get<Order[]>("/api/v1/orders").then(setOrders).catch(() => setOrders([]));
  }, []);

  return (
    <div>
      <h1 className="text-2xl font-bold text-white">Orders</h1>
      {orders === null ? (
        <div className="glass mt-6 h-40 animate-pulse rounded-2xl" aria-busy="true" />
      ) : orders.length === 0 ? (
        <div className="py-16 text-center">
          <Package className="mx-auto h-12 w-12 text-slate-600" aria-hidden />
          <p className="mt-4 text-slate-400">No orders yet.</p>
        </div>
      ) : (
        <div className="mt-6 space-y-4">
          {orders.map((o) => (
            <div key={o.orderNumber} className="glass rounded-2xl p-5">
              <div className="flex flex-wrap items-center justify-between gap-3">
                <div>
                  <span className="font-mono text-sm text-slate-300">{o.orderNumber}</span>
                  <span className={`ml-3 rounded-full px-2.5 py-0.5 text-xs font-bold ${STATUS_STYLES[o.status] ?? "bg-white/5 text-slate-300"}`}>
                    {o.status}
                  </span>
                </div>
                <span className="text-lg font-bold text-white">{formatINR(o.totalMinor)}</span>
              </div>
              <ul className="mt-3 space-y-1 text-sm text-slate-400">
                {o.items.map((i, idx) => (
                  <li key={idx}>{i.title} — {formatINR(i.priceMinor)}</li>
                ))}
              </ul>
              <p className="mt-3 text-xs text-slate-500">Placed {formatDate(o.createdAt)}</p>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
