"use client";

import { useEffect, useState } from "react";
import { api } from "@/lib/api";
import { formatINR } from "@/lib/format";

interface Dashboard {
  revenueTotalMinor: number;
  revenue30dMinor: number;
  ordersTotal: number;
  ordersPaid: number;
  usersTotal: number;
  users30d: number;
  productsPublished: number;
  downloads30d: number;
  averageOrderValueMinor: number;
  conversionRatePercent: number;
  revenueByDay: { day: string; revenue_minor: number; paid_orders: number }[];
  topProducts: { title: string; slug: string; units_sold: number; revenue_minor: number }[];
}

export default function AdminDashboard() {
  const [data, setData] = useState<Dashboard | null>(null);

  useEffect(() => {
    api.get<Dashboard>("/api/v1/admin/dashboard").then(setData).catch(() => setData(null));
  }, []);

  if (!data) return <div className="glass h-64 animate-pulse rounded-2xl" aria-busy="true" />;

  const maxRevenue = Math.max(1, ...data.revenueByDay.map((d) => Number(d.revenue_minor)));

  const cards = [
    { label: "Revenue (all time)", value: formatINR(data.revenueTotalMinor) },
    { label: "Revenue (30 days)", value: formatINR(data.revenue30dMinor) },
    { label: "Paid orders", value: String(data.ordersPaid) },
    { label: "Avg order value", value: formatINR(data.averageOrderValueMinor) },
    { label: "Users", value: `${data.usersTotal} (+${data.users30d})` },
    { label: "Published products", value: String(data.productsPublished) },
    { label: "Downloads (30 days)", value: String(data.downloads30d) },
    { label: "Conversion", value: `${data.conversionRatePercent}%` },
  ];

  return (
    <div>
      <h1 className="text-2xl font-bold text-white">Dashboard</h1>

      <div className="mt-6 grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        {cards.map((c) => (
          <div key={c.label} className="glass rounded-2xl p-5">
            <p className="text-xs uppercase tracking-wide text-slate-500">{c.label}</p>
            <p className="mt-1 text-2xl font-black text-white">{c.value}</p>
          </div>
        ))}
      </div>

      <div className="glass mt-6 rounded-2xl p-5">
        <h2 className="font-semibold text-white">Revenue — last 14 days</h2>
        <div className="mt-4 flex h-36 items-end gap-1.5" role="img" aria-label="Daily revenue chart">
          {data.revenueByDay.map((d) => (
            <div key={String(d.day)} className="group relative flex-1">
              <div
                className="rounded-t bg-gradient-to-t from-accent-600/40 to-accent-400"
                style={{ height: `${Math.max(2, (Number(d.revenue_minor) / maxRevenue) * 130)}px` }}
              />
              <span className="pointer-events-none absolute -top-7 left-1/2 hidden -translate-x-1/2 whitespace-nowrap rounded bg-ink-900 px-2 py-1 text-[10px] text-white group-hover:block">
                {formatINR(Number(d.revenue_minor))}
              </span>
            </div>
          ))}
        </div>
      </div>

      <div className="glass mt-6 rounded-2xl p-5">
        <h2 className="font-semibold text-white">Top products (paid orders)</h2>
        {data.topProducts.length === 0 ? (
          <p className="mt-3 text-sm text-slate-500">No sales yet.</p>
        ) : (
          <table className="mt-3 w-full text-sm">
            <thead className="text-left text-xs uppercase text-slate-500">
              <tr><th className="pb-2">Product</th><th className="pb-2">Units</th><th className="pb-2">Revenue</th></tr>
            </thead>
            <tbody>
              {data.topProducts.map((p) => (
                <tr key={p.slug} className="border-t border-white/5">
                  <td className="py-2 text-slate-200">{p.title}</td>
                  <td className="py-2 text-slate-300">{p.units_sold}</td>
                  <td className="py-2 text-white">{formatINR(Number(p.revenue_minor))}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </div>
  );
}
