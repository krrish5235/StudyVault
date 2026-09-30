import type { Metadata } from "next";
import { BookOpenText, ShieldCheck, RefreshCcw, HeartHandshake } from "lucide-react";

export const metadata: Metadata = { title: "About" };

export default function AboutPage() {
  return (
    <div className="mx-auto max-w-3xl px-4 py-14">
      <h1 className="text-3xl font-bold text-white">About StudyVault</h1>
      <p className="mt-6 text-lg leading-relaxed text-slate-300">
        StudyVault is a curated marketplace of programming study material. Every product is
        written, reviewed and versioned by the store owner — no scraped content, no fluff, just
        the material that actually moves the needle.
      </p>
      <div className="mt-10 grid gap-4 sm:grid-cols-2">
        <div className="glass rounded-2xl p-5">
          <BookOpenText className="h-6 w-6 text-accent-400" aria-hidden />
          <h2 className="mt-3 font-semibold text-white">Depth over volume</h2>
          <p className="mt-1 text-sm text-slate-400">
            Notes distilled from real projects and interviews, organized for revision speed.
          </p>
        </div>
        <div className="glass rounded-2xl p-5">
          <RefreshCcw className="h-6 w-6 text-accent-400" aria-hidden />
          <h2 className="mt-3 font-semibold text-white">Maintained, not abandoned</h2>
          <p className="mt-1 text-sm text-slate-400">
            Products get versioned updates. Buy once, keep receiving improvements.
          </p>
        </div>
        <div className="glass rounded-2xl p-5">
          <ShieldCheck className="h-6 w-6 text-accent-400" aria-hidden />
          <h2 className="mt-3 font-semibold text-white">Secure by design</h2>
          <p className="mt-1 text-sm text-slate-400">
            Encrypted transport, hashed credentials, and signed downloads.
          </p>
        </div>
        <div className="glass rounded-2xl p-5">
          <HeartHandshake className="h-6 w-6 text-accent-400" aria-hidden />
          <h2 className="mt-3 font-semibold text-white">Honest support</h2>
          <p className="mt-1 text-sm text-slate-400">
            Questions about material? Real answers, directly from the author.
          </p>
        </div>
      </div>
    </div>
  );
}
