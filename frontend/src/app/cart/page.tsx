"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { Trash2, ShoppingBag } from "lucide-react";
import { useCart } from "@/components/providers/CartProvider";
import { useSession } from "@/components/providers/SessionProvider";
import { formatINR } from "@/lib/format";

export default function CartPage() {
  const { items, remove, totalMinor, clear } = useCart();
  const { user } = useSession();
  const router = useRouter();

  function checkout() {
    // Prices are always recomputed server-side at checkout; the cart is only a picker.
    router.push("/checkout");
  }

  if (items.length === 0) {
    return (
      <div className="mx-auto max-w-2xl px-4 py-24 text-center">
        <ShoppingBag className="mx-auto h-12 w-12 text-slate-600" aria-hidden />
        <h1 className="mt-4 text-2xl font-bold text-white">Your cart is empty</h1>
        <p className="mt-2 text-slate-400">Browse the catalog and add something worth learning.</p>
        <Link href="/products" className="mt-6 inline-block rounded-xl bg-accent-600 px-6 py-3 font-semibold text-white hover:bg-accent-500">
          Browse catalog
        </Link>
      </div>
    );
  }

  return (
    <div className="mx-auto max-w-4xl px-4 py-10">
      <h1 className="text-3xl font-bold text-white">Your cart</h1>

      <div className="glass mt-6 divide-y divide-white/5 rounded-2xl">
        {items.map((item) => (
          <div key={item.productId} className="flex items-center gap-4 p-4">
            <div className="flex h-14 w-20 items-center justify-center rounded-lg bg-gradient-to-br from-ink-700 to-ink-900 text-lg font-black text-white/20">
              {item.title.slice(0, 2).toUpperCase()}
            </div>
            <div className="flex-1">
              <Link href={`/products/${item.slug}`} className="font-semibold text-white hover:text-accent-400">
                {item.title}
              </Link>
              <p className="text-sm text-slate-500">Digital product · instant download</p>
            </div>
            <span className="font-bold text-white">{formatINR(item.priceMinor)}</span>
            <button
              onClick={() => remove(item.productId)}
              aria-label={`Remove ${item.title} from cart`}
              className="rounded-lg p-2 text-slate-500 hover:bg-red-500/10 hover:text-red-400"
            >
              <Trash2 className="h-4 w-4" aria-hidden />
            </button>
          </div>
        ))}
      </div>

      <div className="glass mt-6 rounded-2xl p-6">
        <div className="flex justify-between text-lg">
          <span className="text-slate-300">Total</span>
          <span className="text-2xl font-black text-white">{formatINR(totalMinor)}</span>
        </div>
        <p className="mt-1 text-xs text-slate-500">Final total is verified server-side at checkout.</p>
        <div className="mt-5 flex flex-col gap-3 sm:flex-row">
          {user ? (
            <button
              onClick={checkout}
              className="flex-1 rounded-xl bg-accent-600 py-3 font-semibold text-white hover:bg-accent-500"
            >
              Proceed to checkout
            </button>
          ) : (
            <Link
              href="/login?next=/cart"
              className="flex-1 rounded-xl bg-accent-600 py-3 text-center font-semibold text-white hover:bg-accent-500"
            >
              Sign in to checkout
            </Link>
          )}
          <button onClick={clear} className="rounded-xl border border-white/15 px-6 py-3 text-slate-300 hover:bg-white/5">
            Clear cart
          </button>
        </div>
      </div>
    </div>
  );
}
