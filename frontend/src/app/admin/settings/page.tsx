"use client";

import { useEffect, useState } from "react";
import { api } from "@/lib/api";

const EDITABLE = ["site.name", "site.tagline", "hero.title", "hero.subtitle", "hero.ctaText", "about.content", "contact.email", "footer.text"];

export default function AdminSettingsPage() {
  const [values, setValues] = useState<Record<string, string>>({});
  const [saved, setSaved] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    api.get<Record<string, unknown>>("/api/v1/settings/site").then((data) => {
      const flat: Record<string, string> = {};
      for (const key of EDITABLE) {
        const v = data[key];
        flat[key] = typeof v === "string" ? v : v == null ? "" : JSON.stringify(v);
      }
      setValues(flat);
    }).catch(() => {});
  }, []);

  async function save() {
    setError(null);
    setSaved(false);
    try {
      await fetch(`${process.env.NEXT_PUBLIC_API_URL ?? ""}/api/v1/admin/settings/site`, {
        method: "PUT", credentials: "include",
        headers: { "Content-Type": "application/json", "X-XSRF-TOKEN": document.cookie.match(/sv_csrf=([^;]+)/)?.[1] ?? "" },
        body: JSON.stringify(values),
      }).then(async (r) => {
        if (!r.ok) throw new Error((await r.json()).message ?? "Save failed");
      });
      setSaved(true);
      setTimeout(() => setSaved(false), 2500);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Save failed.");
    }
  }

  return (
    <div>
      <h1 className="text-2xl font-bold text-white">Website content</h1>
      <p className="mt-1 text-sm text-slate-400">Changes go live without a deploy — served from the database.</p>

      <div className="glass mt-6 space-y-4 rounded-2xl p-6">
        {EDITABLE.map((key) => (
          <div key={key}>
            <label htmlFor={key} className="block text-xs font-medium uppercase tracking-wide text-slate-500">{key}</label>
            {key === "about.content" ? (
              <textarea id={key} rows={4} value={values[key] ?? ""} onChange={(e) => setValues({ ...values, [key]: e.target.value })}
                className="mt-1 w-full rounded-xl border border-white/10 bg-ink-800/60 px-4 py-2.5 text-sm text-white" />
            ) : (
              <input id={key} value={values[key] ?? ""} onChange={(e) => setValues({ ...values, [key]: e.target.value })}
                className="mt-1 w-full rounded-xl border border-white/10 bg-ink-800/60 px-4 py-2.5 text-sm text-white" />
            )}
          </div>
        ))}
        {error && <p role="alert" className="rounded-xl border border-red-500/30 bg-red-500/10 px-4 py-3 text-sm text-red-300">{error}</p>}
        {saved && <p className="rounded-xl border border-mint-400/30 bg-mint-400/10 px-4 py-3 text-sm text-mint-400">Saved.</p>}
        <button onClick={() => void save()} className="rounded-xl bg-accent-600 px-6 py-2.5 font-semibold text-white hover:bg-accent-500">
          Save settings
        </button>
      </div>
    </div>
  );
}
