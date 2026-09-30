"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { CreditCard, Loader2, ShieldCheck, Tag } from "lucide-react";
import { useCart } from "@/components/providers/CartProvider";
import { useSession } from "@/components/providers/SessionProvider";
import { api, ApiError } from "@/lib/api";
import { formatINR } from "@/lib/format";

interface CheckoutIntent {
  orderNumber: string;
  providerOrderId: string;
  providerKeyId: string;
  amountMinor: number;
  currency: string;
}

declare global {
  interface Window {
    Razorpay?: new (options: Record<string, unknown>) => { open: () => void };
  }
}

function loadRazorpayScript(): Promise<boolean> {
  return new Promise((resolve) => {
    if (window.Razorpay) return resolve(true);
    const script = document.createElement("script");
    script.src = "https://checkout.razorpay.com/v1/checkout.js";
    script.onload = () => resolve(true);
    script.onerror = () => resolve(false);
    document.body.appendChild(script);
  });
}

export default function CheckoutPage() {
  const { items, clear } = useCart();
  const { user } = useSession();
  const router = useRouter();
  const [couponCode, setCouponCode] = useState("");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!user) router.replace("/login?next=/checkout");
  }, [user, router]);

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

  const cartTotal = items.reduce((s, i) => s + i.priceMinor, 0);

  async function verify(orderNumber: string, orderId: string, paymentId: string, signature: string) {
    await api.post("/api/v1/payments/verify", {
      orderNumber,
      razorpayOrderId: orderId,
      razorpayPaymentId: paymentId,
      razorpaySignature: signature,
    });
  }

  async function checkout() {
    setBusy(true);
    setError(null);
    try {
      const intent = await api.post<CheckoutIntent>("/api/v1/payments/orders", {
        productIds: items.map((i) => i.productId),
        couponCode: couponCode.trim() || undefined,
      });

      // Dev/mock gateway: keyId is empty - verify with the dev signature token.
      if (!intent.providerKeyId) {
        await verify(intent.orderNumber, intent.providerOrderId, "pay_local_" + Date.now(), "mock-signature");
        clear();
        router.push("/account/library?purchased=1");
        return;
      }

      const ok = await loadRazorpayScript();
      if (!ok) throw new Error("Could not load the payment module. Check your connection.");
      const rzp = new window.Razorpay!({
        key: intent.providerKeyId,
        amount: intent.amountMinor,
        currency: intent.currency,
        name: "StudyVault",
        description: "Study material purchase",
        order_id: intent.providerOrderId,
        prefill: { email: user?.email, name: user?.fullName },
        theme: { color: "#5b6cff" },
        handler: async (response: { razorpay_order_id: string; razorpay_payment_id: string; razorpay_signature: string }) => {
          try {
            await verify(intent.orderNumber, response.razorpay_order_id, response.razorpay_payment_id, response.razorpay_signature);
            clear();
            router.push("/account/library?purchased=1");
          } catch (err) {
            setError(err instanceof ApiError ? err.message : "Payment verification failed.");
            setBusy(false);
          }
        },
      });
      rzp.open();
      setBusy(false);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Could not start checkout. Please try again.");
      setBusy(false);
    }
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

      <div className="glass mt-4 rounded-2xl p-5">
        <label htmlFor="coupon" className="flex items-center gap-2 text-sm text-slate-300">
          <Tag className="h-4 w-4 text-accent-400" aria-hidden /> Coupon code (optional)
        </label>
        <input
          id="coupon"
          value={couponCode}
          onChange={(e) => setCouponCode(e.target.value.toUpperCase())}
          placeholder="e.g. LAUNCH20"
          className="mt-2 w-full rounded-xl border border-white/10 bg-ink-800/60 px-4 py-2.5 text-sm text-white placeholder:text-slate-600"
        />
        <p className="mt-3 flex justify-between text-lg">
          <span className="text-slate-300">Payable (server-verified)</span>
          <span className="font-bold text-white">{formatINR(cartTotal)}</span>
        </p>
        <p className="mt-1 text-xs text-slate-500">The final amount is computed on the server after coupon validation.</p>
      </div>

      {error && (
        <p role="alert" className="mt-4 rounded-xl border border-red-500/30 bg-red-500/10 px-4 py-3 text-sm text-red-300">
          {error}
        </p>
      )}

      <button
        onClick={() => void checkout()}
        disabled={busy}
        className="mt-6 flex w-full items-center justify-center gap-2 rounded-xl bg-accent-600 py-3.5 font-semibold text-white shadow-lg shadow-accent-600/25 hover:bg-accent-500 disabled:opacity-50"
      >
        {busy ? <Loader2 className="h-5 w-5 animate-spin" aria-hidden /> : <CreditCard className="h-5 w-5" aria-hidden />}
        {busy ? "Processing…" : `Pay securely · ${formatINR(cartTotal)}`}
      </button>
      <p className="mt-3 flex items-center justify-center gap-1.5 text-xs text-slate-500">
        <ShieldCheck className="h-3.5 w-3.5" aria-hidden /> Payments are processed by Razorpay. Card details never touch our servers.
      </p>
    </div>
  );
}
