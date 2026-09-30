"use client";

import { useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { Check, ShoppingCart } from "lucide-react";
import { useCart } from "@/components/providers/CartProvider";
import { useSession } from "@/components/providers/SessionProvider";
import type { ProductDetail } from "@/lib/types";

export default function BuyPanel({ product }: { product: ProductDetail }) {
  const { add, has } = useCart();
  const { user } = useSession();
  const router = useRouter();
  const inCart = has(product.id);
  const [justAdded, setJustAdded] = useState(false);

  function addToCart() {
    add({
      productId: product.id,
      slug: product.slug,
      title: product.title,
      priceMinor: product.priceMinor,
      coverImageKey: product.coverImageKey,
    });
    setJustAdded(true);
    setTimeout(() => setJustAdded(false), 1600);
  }

  function buyNow() {
    addToCart();
    router.push("/cart");
  }

  if (inCart || justAdded) {
    return (
      <div className="mt-5 space-y-3">
        <p className="flex items-center gap-2 rounded-xl bg-mint-400/10 px-4 py-3 text-sm text-mint-400">
          <Check className="h-4 w-4" aria-hidden /> Added to cart
        </p>
        <Link
          href="/cart"
          className="block rounded-xl bg-accent-600 px-4 py-3 text-center font-semibold text-white hover:bg-accent-500"
        >
          Go to cart
        </Link>
      </div>
    );
  }

  return (
    <div className="mt-5 space-y-3">
      <button
        onClick={buyNow}
        className="w-full rounded-xl bg-accent-600 px-4 py-3 font-semibold text-white shadow-lg shadow-accent-600/25 transition hover:bg-accent-500"
      >
        Buy now
      </button>
      <button
        onClick={addToCart}
        className="flex w-full items-center justify-center gap-2 rounded-xl border border-white/15 px-4 py-3 font-semibold text-slate-200 transition hover:bg-white/5"
      >
        <ShoppingCart className="h-4 w-4" aria-hidden /> Add to cart
      </button>
    </div>
  );
}
