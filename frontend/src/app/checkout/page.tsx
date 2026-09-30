"use client";

import Link from "next/link";
import { useCart } from "@/components/providers/CartProvider";
import { formatINR } from "@/lib/format";
import { CreditCard } from "lucide-react";

/** Checkout is finalized when the payment provider is connected (see docs/DEPLOYMENT.md). */
export default function CheckoutPage() {
  const { items, totalMinor } = useCart();

  if (items.length === 0) {
    return (
      <div className="mx-auto max-w-2xl px-4 py-24 text-center">
        <h1 className="text-2xl font-bold text-white">Nothing to check out</h1>
        <Link href="/products" className="mt-6 inline-block rounded-xl bg-accent-600 px-6 py-3 font-semibold text-white hover:bg-accent-500">
          Browse catalog
        </Link>
      </div>
    );
  }

  return (
    <div className="mx-auto max-w-2xl px-4 py-14">
      <h1 className="text-3xl font-bold text-white">Checkout</h1>
      <ul className="glass mt-6 divide-y divide-white/5 rounded-2xl">
        {items.map((i) => (
          <li key={i.productId} className="flex justify-between p-4 text-sm">
            <span className="text-slate-200">{i.title}</span>
            <span className="text-white">{formatINR(i.priceMinor)}</span>
          </li>
        ))}
      </ul>
      <div className="glass mt-4 flex justify-between rounded-2xl p-5 text-lg">
        <span className="text-slate-300">Total</span>
        <span className="font-bold text-white">{formatINR(totalMinor)}</span>
      </div>
      <button
        disabled
        title="Payment provider configuration pending"
        className="mt-6 flex w-full items-center justify-center gap-2 rounded-xl bg-ink-600 py-3 font-semibold text-slate-400"
      >
        <CreditCard className="h-5 w-5" aria-hidden /> Online payments are being configured
      </button>
      <p className="mt-3 text-center text-xs text-slate-500">
        Prices shown are informational; the final amount is computed and verified server-side.
      </p>
    </div>
  );
}
