"use client";

import { useState } from "react";
import { ShieldCheck } from "lucide-react";

/**
 * Renders product content with copy protection: text selection is disabled and
 * copy attempts show a ownership notice. (Client-side protection is a deterrent,
 * not DRM - the real enforcement is the license in the terms.)
 */
export default function DescriptionGuard({ children }: { children: React.ReactNode }) {
  const [warning, setWarning] = useState(false);

  function blockCopy(e: React.ClipboardEvent) {
    e.preventDefault();
    setWarning(true);
    setTimeout(() => setWarning(false), 2600);
  }

  function blockContextMenu(e: React.MouseEvent) {
    e.preventDefault();
    setWarning(true);
    setTimeout(() => setWarning(false), 2600);
  }

  return (
    <div
      className="protected relative"
      onCopy={blockCopy}
      onCut={blockCopy}
      onContextMenu={blockContextMenu}
    >
      {children}
      {warning && (
        <div
          role="alert"
          className="fixed bottom-6 left-1/2 z-50 flex -translate-x-1/2 items-center gap-2 rounded-xl border border-accent-400/40 bg-ink-900/95 px-5 py-3 text-sm text-slate-200 shadow-2xl"
        >
          <ShieldCheck className="h-4 w-4 text-accent-400" aria-hidden />
          Content protected © StudyVault — Developed by Krrish
        </div>
      )}
    </div>
  );
}
