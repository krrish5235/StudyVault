"use client";

import { useCallback, useEffect, useState } from "react";
import { api } from "@/lib/api";
import { formatDate } from "@/lib/format";
import type { Page } from "@/lib/types";

interface AdminUser {
  id: string; email: string; fullName: string; emailVerified: boolean;
  enabled: boolean; mustChangePassword: boolean; roles: string[];
  createdAt: string; lastLoginAt: string | null;
}

export default function AdminUsersPage() {
  const [users, setUsers] = useState<Page<AdminUser> | null>(null);
  const [query, setQuery] = useState("");

  const load = useCallback(() => {
    const q = query.trim() ? `&query=${encodeURIComponent(query.trim())}` : "";
    api.get<Page<AdminUser>>(`/api/v1/admin/users?size=50${q}`).then(setUsers).catch(() => setUsers(null));
  }, [query]);

  useEffect(load, [load]);

  async function setEnabled(id: string, enabled: boolean) {
    if (!enabled && !window.confirm("Disable this account? All their sessions will be signed out.")) return;
    try {
      await fetch(`${process.env.NEXT_PUBLIC_API_URL ?? ""}/api/v1/admin/users/${id}/enabled`, {
        method: "PATCH", credentials: "include",
        headers: {
          "Content-Type": "application/json",
          "X-XSRF-TOKEN": document.cookie.match(/sv_csrf=([^;]+)/)?.[1] ?? "",
        },
        body: JSON.stringify({ enabled }),
      });
      load();
    } catch {
      /* reload shows state */
    }
  }

  return (
    <div>
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold text-white">Users</h1>
        <input value={query} onChange={(e) => setQuery(e.target.value)} placeholder="Search email or name…"
          className="w-64 rounded-xl border border-white/10 bg-ink-800/60 px-4 py-2 text-sm text-white placeholder:text-slate-600" />
      </div>

      <div className="glass mt-6 overflow-x-auto rounded-2xl">
        <table className="w-full text-sm">
          <thead className="text-left text-xs uppercase text-slate-500">
            <tr><th className="p-4">User</th><th className="p-4">Roles</th><th className="p-4">Status</th><th className="p-4">Joined</th><th className="p-4"></th></tr>
          </thead>
          <tbody>
            {users?.content.map((u) => (
              <tr key={u.id} className="border-t border-white/5">
                <td className="p-4">
                  <p className="font-semibold text-white">{u.fullName}</p>
                  <p className="text-xs text-slate-500">{u.email}</p>
                </td>
                <td className="p-4 text-xs text-slate-300">{u.roles.join(", ")}</td>
                <td className="p-4 text-xs">
                  {u.enabled ? <span className="text-mint-400">active</span> : <span className="text-red-400">disabled</span>}
                  {!u.emailVerified && <span className="ml-2 text-gold-400">unverified</span>}
                </td>
                <td className="p-4 text-xs text-slate-400">{formatDate(u.createdAt)}</td>
                <td className="p-4">
                  <button onClick={() => void setEnabled(u.id, !u.enabled)}
                    className={`rounded-lg px-3 py-1.5 text-xs font-semibold ${
                      u.enabled ? "bg-red-500/10 text-red-300 hover:bg-red-500/20" : "bg-mint-400/10 text-mint-400 hover:bg-mint-400/20"}`}>
                    {u.enabled ? "Disable" : "Enable"}
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
