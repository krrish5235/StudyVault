"use client";

import { useState } from "react";
import { Terminal, Loader2, AlertTriangle } from "lucide-react";
import { api, ApiError } from "@/lib/api";

interface CommandResponse {
  kind: "data" | "confirm_required" | "done";
  summary: string;
  data: unknown;
  confirmToken: string | null;
}

const SUGGESTIONS = [
  "show revenue",
  "show today's orders",
  "find user admin@studyvault.local",
  "change price of <product> to 299",
  "publish <product slug>",
  "archive product <product>",
];

export default function CommandCenterPage() {
  const [text, setText] = useState("");
  const [history, setHistory] = useState<{ role: "you" | "vault"; text: string; kind?: string; data?: unknown; token?: string }[]>([]);
  const [busy, setBusy] = useState(false);

  async function send(command?: string) {
    const cmd = (command ?? text).trim();
    if (!cmd || busy) return;
    setHistory((h) => [...h, { role: "you", text: cmd }]);
    setText("");
    setBusy(true);
    try {
      const res = await api.post<CommandResponse>("/api/v1/admin/command", { text: cmd });
      setHistory((h) => [...h, { role: "vault", text: res.summary, kind: res.kind, data: res.data, token: res.confirmToken ?? undefined }]);
    } catch (err) {
      setHistory((h) => [...h, { role: "vault", text: err instanceof ApiError ? err.message : "Command failed.", kind: "error" }]);
    } finally {
      setBusy(false);
    }
  }

  async function confirm(token: string) {
    setBusy(true);
    try {
      const res = await api.post<CommandResponse>("/api/v1/admin/command/confirm", { confirmToken: token });
      setHistory((h) => [...h, { role: "vault", text: res.summary, kind: "done" }]);
    } catch (err) {
      setHistory((h) => [...h, { role: "vault", text: err instanceof ApiError ? err.message : "Confirmation failed.", kind: "error" }]);
    } finally {
      setBusy(false);
    }
  }

  return (
    <div>
      <h1 className="flex items-center gap-2 text-2xl font-bold text-white">
        <Terminal className="h-6 w-6 text-accent-400" aria-hidden /> Command Center
      </h1>
      <p className="mt-1 text-sm text-slate-400">
        Natural language, mapped to whitelisted actions only. Destructive commands always ask for confirmation.
      </p>

      <div className="glass mt-6 min-h-[320px] space-y-3 rounded-2xl p-5">
        {history.length === 0 && (
          <div className="flex flex-wrap gap-2">
            {SUGGESTIONS.map((s) => (
              <button key={s} onClick={() => void send(s)}
                className="rounded-full border border-white/10 bg-white/5 px-3 py-1.5 text-xs text-slate-300 hover:border-accent-400 hover:text-white">
                {s}
              </button>
            ))}
          </div>
        )}
        {history.map((m, i) => (
          <div key={i} className={m.role === "you" ? "text-right" : ""}>
            <span className={`inline-block max-w-[85%] rounded-2xl px-4 py-2.5 text-left text-sm ${
              m.role === "you" ? "bg-accent-600 text-white"
              : m.kind === "error" ? "border border-red-500/30 bg-red-500/10 text-red-300"
              : m.kind === "confirm_required" ? "border border-gold-500/40 bg-gold-500/10 text-gold-400"
              : "bg-white/5 text-slate-200"}`}>
              {m.kind === "confirm_required" && <AlertTriangle className="mr-1.5 inline h-4 w-4" aria-hidden />}
              {m.text}
              {m.data != null && (
                <pre className="mt-2 max-h-40 overflow-auto rounded-lg bg-ink-900/80 p-2 text-left text-[11px] text-slate-400">
                  {JSON.stringify(m.data, null, 2)}
                </pre>
              )}
              {m.token && (
                <button onClick={() => void confirm(m.token!)} disabled={busy}
                  className="mt-2 block w-full rounded-lg bg-accent-600 px-3 py-1.5 text-xs font-bold text-white hover:bg-accent-500 disabled:opacity-50">
                  Yes, execute
                </button>
              )}
            </span>
          </div>
        ))}
        {busy && <Loader2 className="h-4 w-4 animate-spin text-slate-500" aria-hidden />}
      </div>

      <form onSubmit={(e) => { e.preventDefault(); void send(); }} className="mt-4 flex gap-3">
        <input
          value={text}
          onChange={(e) => setText(e.target.value)}
          placeholder='Type a command… e.g. "show revenue"'
          aria-label="Command"
          className="flex-1 rounded-xl border border-white/10 bg-ink-800/60 px-4 py-3 text-sm text-white placeholder:text-slate-600"
        />
        <button type="submit" disabled={busy} className="rounded-xl bg-accent-600 px-6 font-semibold text-white hover:bg-accent-500 disabled:opacity-50">
          Run
        </button>
      </form>
    </div>
  );
}
