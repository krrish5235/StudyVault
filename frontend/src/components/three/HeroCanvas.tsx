"use client";

import dynamic from "next/dynamic";
import { useEffect, useState } from "react";

const HeroScene = dynamic(() => import("./HeroScene"), {
  ssr: false,
  loading: () => <div className="h-full w-full" aria-hidden />,
});

/**
 * Progressive enhancement wrapper: renders the 3D scene only on capable
 * devices and respects prefers-reduced-motion; falls back to a static
 * gradient composition everywhere else.
 */
export default function HeroCanvas() {
  const [enabled, setEnabled] = useState(false);

  useEffect(() => {
    const reduced = window.matchMedia("(prefers-reduced-motion: reduce)").matches;
    const lowPower =
      typeof navigator.hardwareConcurrency === "number" && navigator.hardwareConcurrency <= 2;
    const smallScreen = window.innerWidth < 768;
    setEnabled(!reduced && !(smallScreen && lowPower));
  }, []);

  if (!enabled) {
    return (
      <div className="relative h-full w-full" aria-hidden>
        <div className="absolute left-[12%] top-[20%] h-36 w-24 rotate-12 rounded-xl bg-gradient-to-br from-accent-500/50 to-accent-600/10 blur-[1px]" />
        <div className="absolute right-[15%] top-[35%] h-40 w-28 -rotate-6 rounded-xl bg-gradient-to-br from-gold-500/40 to-gold-500/5" />
        <div className="absolute bottom-[15%] left-[38%] h-28 w-20 rotate-3 rounded-xl bg-gradient-to-br from-mint-400/30 to-mint-400/5" />
      </div>
    );
  }

  return (
    <div className="h-full w-full">
      <HeroScene />
    </div>
  );
}
