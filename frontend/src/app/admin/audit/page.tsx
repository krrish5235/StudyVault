"use client";

import { useCallback, useEffect, useState } from "react";
import { api } from "@/lib/api";
import type { Page } from "@/lib/types";

interface AuditEntry {
  createdAt: string; actorEmail: string | null; action: string;
  resourceType: string | null; resourceId: string | null;
  details: Record<string, unknown> | null; ipAddress: string | null;
}

export default function AdminAuditPage() {
  const [logs, setLogs] = useState<Page<AuditEntry> | null>(null);
  const [page, setPage] = useState(0);

  const load = useCallback(() => {
    api.get<Page<AuditEntry>>(`/api/v1/admin/audit-logs?page=${page}&size=30`).then(setLogs).catch(() => setLogs(null));
  }, [page]);

  useEffect(load, [load]);

  return (
    <div>
      <h1 className="text-2xl font-bold text-white">Audit log</h1>
      <p className="mt-1 text-sm text-slate-400">Who did what, when — immutable trail of privileged actions.</p>

      <div className="glass mt-6 overflow-x-auto rounded-2xl">
        <table className="w-full text-sm">
          <thead className="text-left text-xs uppercase text-slate-500">
            <tr><th className="p-4">When</th><th className="p-4">Actor</th><th className="p-4">Action</th><th className="p-4">Resource</th><th className="p-4">Details</th></tr>
          </thead>
          <tbody>
            {logs?.content.map((l, i) => (
              <tr key={i} className="border-t border-white/5">
                <td className="p-4 whitespace-nowrap text-xs text-slate-400">{new Date(l.createdAt).toLocaleString("en-IN")}</td>
                <td className="p-4 text-xs text-slate-300">{l.actorEmail ?? "anonymous"}</td>
                <td className="p-4"><span className="rounded bg-white/5 px-2 py-0.5 font-mono text-xs text-accent-400">{l.action}</span></td>
                <td className="p-4 text-xs text-slate-400">{l.resourceType}:{l.resourceId?.slice(0, 8)}</td>
                <td className="p-4 max-w-xs truncate font-mono text-[11px] text-slate-500">{JSON.stringify(l.details)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <div className="mt-4 flex items-center justify-center gap-3">
        <button disabled={page === 0} onClick={() => setPage((p) => p - 1)} className="rounded-lg border border-white/10 px-4 py-2 text-sm disabled:opacity-40">Previous</button>
        <span className="text-sm text-slate-400">Page {page + 1} of {Math.max(1, logs?.totalPages ?? 1)}</span>
        <button disabled={(logs?.totalPages ?? 1) <= page + 1} onClick={() => setPage((p) => p + 1)} className="rounded-lg border border-white/10 px-4 py-2 text-sm disabled:opacity-40">Next</button>
      </div>
    </div>
  );
}
