"use client";

import { useCallback, useEffect, useState } from "react";
import { api } from "@/lib/api";
import { formatDate } from "@/lib/format";
import type { Page } from "@/lib/types";

interface AdminReview {
  id: string; productTitle: string; userEmail: string; rating: number;
  comment: string | null; status: string; createdAt: string;
}

export default function AdminReviewsPage() {
  const [reviews, setReviews] = useState<Page<AdminReview> | null>(null);

  const load = useCallback(() => {
    api.get<Page<AdminReview>>("/api/v1/admin/reviews?size=50").then(setReviews).catch(() => setReviews(null));
  }, []);

  useEffect(load, [load]);

  async function moderate(id: string, action: "hide" | "publish") {
    await fetch(`${process.env.NEXT_PUBLIC_API_URL ?? ""}/api/v1/admin/reviews/${id}/${action}`, {
      method: "POST", credentials: "include",
      headers: { "X-XSRF-TOKEN": document.cookie.match(/sv_csrf=([^;]+)/)?.[1] ?? "" },
    });
    load();
  }

  return (
    <div>
      <h1 className="text-2xl font-bold text-white">Reviews</h1>
      <div className="mt-6 space-y-4">
        {reviews?.content.map((r) => (
          <div key={r.id} className="glass rounded-2xl p-5">
            <div className="flex flex-wrap items-center justify-between gap-2">
              <div>
                <p className="font-semibold text-white">{r.productTitle}</p>
                <p className="text-xs text-slate-500">{r.userEmail} · {formatDate(r.createdAt)} ·
                  <span className={`ml-2 font-bold ${r.status === "PUBLISHED" ? "text-mint-400" : "text-slate-500"}`}>{r.status}</span>
                </p>
              </div>
              <span className="text-lg font-bold text-gold-400">{"★".repeat(r.rating)}</span>
            </div>
            {r.comment && <p className="mt-2 text-sm text-slate-300">{r.comment}</p>}
            <div className="mt-3 flex gap-2">
              {r.status === "PUBLISHED" ? (
                <button onClick={() => void moderate(r.id, "hide")} className="rounded-lg bg-red-500/10 px-3 py-1.5 text-xs text-red-300 hover:bg-red-500/20">Hide</button>
              ) : (
                <button onClick={() => void moderate(r.id, "publish")} className="rounded-lg bg-mint-400/10 px-3 py-1.5 text-xs text-mint-400 hover:bg-mint-400/20">Publish</button>
              )}
            </div>
          </div>
        ))}
        {reviews?.content.length === 0 && <p className="text-sm text-slate-500">No reviews yet.</p>}
      </div>
    </div>
  );
}
