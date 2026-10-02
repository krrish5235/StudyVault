"use client";

import { useCallback, useEffect, useState } from "react";
import { BadgeCheck, XCircle, IndianRupee } from "lucide-react";
import { api } from "@/lib/api";
import { formatINR } from "@/lib/format";
import type { Page } from "@/lib/types";

interface AdminRequest {
  id: string;
  orderNumber: string;
  userEmail: string;
  totalMinor: number;
  currency: string;
  transactionRef: string;
  status: "PENDING" | "APPROVED" | "REJECTED";
  submittedAt: string;
  reviewedAt: string | null;
  adminNote: string | null;
}

const FILTERS = ["", "PENDING", "APPROVED", "REJECTED"] as const;

export default function AdminPaymentsPage() {
  const [requests, setRequests] = useState<Page<AdminRequest> | null>(null);
  const [status, setStatus] = useState<string>("PENDING");
  const [error, setError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);

  const load = useCallback(() => {
    const q = status ? `&status=${status}` : "";
    api.get<Page<AdminRequest>>(`/api/v1/admin/payment-requests?size=50${q}`)
      .then(setRequests)
      .catch(() => setRequests(null));
  }, [status]);

  useEffect(load, [load]);

  async function review(id: string, action: "approve" | "reject") {
    if (action === "approve" && !window.confirm("Approve this payment? The buyer gets access immediately.")) return;
    let note: string | null = null;
    if (action === "reject") {
      note = window.prompt("Reason for rejection (shown to no one, kept in the audit trail):") ?? "";
      if (note === null) return;
    }
    setError(null);
    setNotice(null);
    try {
      const res = await api.raw(`/api/v1/admin/payment-requests/${id}/${action}`, "POST", note ? { note } : undefined);
      const msg = (res as { message?: string } | null)?.message ?? "Done.";
      setNotice(msg);
      setTimeout(() => setNotice(null), 4000);
      load();
    } catch (err) {
      setError(err instanceof Error ? err.message : "Action failed.");
      setTimeout(() => setError(null), 5000);
    }
  }

  return (
    <div>
      <div className="flex flex-wrap items-center justify-between gap-3">
        <h1 className="flex items-center gap-2 text-2xl font-bold text-white">
          <IndianRupee className="h-6 w-6 text-accent-400" aria-hidden /> Payment requests
        </h1>
        <div className="flex gap-1.5" role="group" aria-label="Filter by status">
          {FILTERS.map((f) => (
            <button key={f || "ALL"} onClick={() => setStatus(f)}
              className={`rounded-lg px-3.5 py-2 text-xs font-bold transition ${
                status === f ? "bg-accent-600 text-white" : "bg-white/5 text-slate-400 hover:text-white"}`}>
              {f || "ALL"}
            </button>
          ))}
        </div>
      </div>
      <p className="mt-1 text-sm text-slate-400">
        Buyers paid via UPI and submitted their transaction ID. Verify in your UPI app, then approve to grant access.
      </p>

      {notice && <p className="mt-4 rounded-xl border border-mint-400/30 bg-mint-400/10 px-4 py-3 text-sm text-mint-400">{notice}</p>}
      {error && <p role="alert" className="mt-4 rounded-xl border border-red-500/30 bg-red-500/10 px-4 py-3 text-sm text-red-300">{error}</p>}

      <div className="glass mt-6 overflow-hidden rounded-2xl">
        <table className="w-full text-sm">
          <thead className="text-left text-xs uppercase text-slate-500">
            <tr>
              <th className="p-4">Order</th><th className="p-4">Buyer</th><th className="p-4">Amount</th>
              <th className="p-4">Transaction ID</th><th className="p-4">Status</th><th className="p-4">Actions</th>
            </tr>
          </thead>
          <tbody>
            {requests?.content.map((r) => (
              <tr key={r.id} className="border-t border-white/5">
                <td className="p-4 font-mono text-xs text-slate-300">{r.orderNumber}</td>
                <td className="p-4 text-slate-300">{r.userEmail}</td>
                <td className="p-4 font-bold text-white">{formatINR(r.totalMinor)}</td>
                <td className="p-4 font-mono text-xs text-accent-400">{r.transactionRef}</td>
                <td className="p-4">
                  <span className={`rounded-full px-2.5 py-0.5 text-xs font-bold ${
                    r.status === "PENDING" ? "bg-gold-500/10 text-gold-400"
                    : r.status === "APPROVED" ? "bg-mint-400/10 text-mint-400" : "bg-red-500/10 text-red-300"}`}>
                    {r.status}
                  </span>
                </td>
                <td className="p-4">
                  {r.status === "PENDING" ? (
                    <div className="flex gap-2">
                      <button onClick={() => void review(r.id, "approve")}
                        className="flex items-center gap-1 rounded-lg bg-mint-400/10 px-3 py-1.5 text-xs font-semibold text-mint-400 hover:bg-mint-400/20">
                        <BadgeCheck className="h-3 w-3" aria-hidden /> Approve
                      </button>
                      <button onClick={() => void review(r.id, "reject")}
                        className="flex items-center gap-1 rounded-lg bg-red-500/10 px-3 py-1.5 text-xs text-red-300 hover:bg-red-500/20">
                        <XCircle className="h-3 w-3" aria-hidden /> Reject
                      </button>
                    </div>
                  ) : (
                    <span className="text-xs text-slate-500">{r.adminNote ?? "reviewed"}</span>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
        {requests?.content.length === 0 && <p className="p-6 text-sm text-slate-500">No payment requests here.</p>}
      </div>
    </div>
  );
}
