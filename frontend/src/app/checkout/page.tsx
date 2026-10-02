"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import {
  CreditCard, Loader2, ShieldCheck, Tag, Copy, Check,
  QrCode, Clock3, BadgeCheck, XCircle,
} from "lucide-react";
import { useCart } from "@/components/providers/CartProvider";
import { useSession } from "@/components/providers/SessionProvider";
import { api, ApiError } from "@/lib/api";
import { formatINR } from "@/lib/format";

interface CheckoutIntent {
  orderNumber: string;
  providerOrderId: string;
  providerKeyId: string;
  provider: string;
  amountMinor: number;
  currency: string;
}

declare global {
  interface Window {
    Razorpay?: new (options: Record<string, unknown>) => { open: () => void };
  }
}

type UpiStage = "pay" | "submitted";

export default function CheckoutPage() {
  const { items, clear } = useCart();
  const { user } = useSession();
  const router = useRouter();

  const [intent, setIntent] = useState<CheckoutIntent | null>(null);
  const [upiId, setUpiId] = useState("");
  const [payeeName, setPayeeName] = useState("");
  const [stage, setStage] = useState<"review" | "upi">("review");
  const [upiStage, setUpiStage] = useState<UpiStage>("pay");
  const [txnRef, setTxnRef] = useState("");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [copied, setCopied] = useState(false);

  useEffect(() => {
    if (!user) router.replace("/login?next=/checkout");
  }, [user, router]);

  // UPI details come from DB-backed site settings (admin editable).
  useEffect(() => {
    api.get<Record<string, unknown>>("/api/v1/settings/site").then((s) => {
      setUpiId(typeof s["payment.upiId"] === "string" ? (s["payment.upiId"] as string) : "");
      setPayeeName(typeof s["payment.payeeName"] === "string" ? (s["payment.payeeName"] as string) : "");
    }).catch(() => {});
  }, []);

  const cartTotal = items.reduce((s, i) => s + i.priceMinor, 0);

  async function startPayment() {
    setBusy(true);
    setError(null);
    try {
      const result = await api.post<CheckoutIntent>("/api/v1/payments/orders", {
        productIds: items.map((i) => i.productId),
        couponCode: undefined,
      });
      setIntent(result);
      setStage(result.provider === "upi" ? "upi" : "review");
      if (result.provider === "razorpay") {
        await openRazorpay(result);
      }
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Could not start checkout. Please try again.");
    } finally {
      setBusy(false);
    }
  }

  async function openRazorpay(intent: CheckoutIntent) {
    const ok = await new Promise<boolean>((resolve) => {
      if (window.Razorpay) return resolve(true);
      const script = document.createElement("script");
      script.src = "https://checkout.razorpay.com/v1/checkout.js";
      script.onload = () => resolve(true);
      script.onerror = () => resolve(false);
      document.body.appendChild(script);
    });
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
          await api.post("/api/v1/payments/verify", {
            orderNumber: intent.orderNumber,
            razorpayOrderId: response.razorpay_order_id,
            razorpayPaymentId: response.razorpay_payment_id,
            razorpaySignature: response.razorpay_signature,
          });
          clear();
          router.push("/account/library?purchased=1");
        } catch (err) {
          setError(err instanceof ApiError ? err.message : "Payment verification failed.");
        }
      },
    });
    rzp.open();
  }

  async function submitClaim() {
    if (!intent) return;
    setBusy(true);
    setError(null);
    try {
      await api.post("/api/v1/payments/upi/claim", {
        orderNumber: intent.orderNumber,
        transactionRef: txnRef.trim(),
      });
      setUpiStage("submitted");
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Could not submit the payment details.");
    } finally {
      setBusy(false);
    }
  }

  function copyUpi() {
    void navigator.clipboard.writeText(upiId);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  }

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

  // ---------------- UPI panel ----------------
  if (stage === "upi" && intent) {
    return (
      <div className="mx-auto max-w-2xl px-4 py-14">
        <h1 className="text-3xl font-bold text-white">Pay via UPI</h1>
        <p className="mt-1 text-sm text-slate-400">
          Order <span className="font-mono text-slate-300">{intent.orderNumber}</span> ·{" "}
          <span className="font-bold text-white">{formatINR(intent.amountMinor)}</span>
        </p>

        {upiStage === "pay" ? (
          <>
            <div className="glass mt-6 grid gap-6 rounded-2xl p-6 sm:grid-cols-[220px_1fr]">
              <div className="mx-auto w-52 overflow-hidden rounded-2xl border border-white/10 bg-white p-2">
                {/* eslint-disable-next-line @next/next/no-img-element */}
                <img src="/upi-qr.jpeg" alt="UPI QR code" className="h-full w-full object-contain" />
              </div>
              <div className="flex flex-col justify-center gap-3 text-sm">
                <p className="flex items-center gap-2 text-slate-300">
                  <QrCode className="h-4 w-4 text-accent-400" aria-hidden /> Scan with any UPI app
                </p>
                {payeeName && <p className="text-slate-300">Pay to: <span className="font-semibold text-white">{payeeName}</span></p>}
                <div>
                  <p className="text-slate-400">UPI ID</p>
                  <div className="mt-1 flex items-center gap-2">
                    <code className="rounded-lg bg-ink-900/80 px-3 py-2 text-sm text-accent-400">{upiId}</code>
                    <button onClick={copyUpi} aria-label="Copy UPI ID"
                      className="rounded-lg border border-white/15 p-2 text-slate-300 hover:bg-white/5">
                      {copied ? <Check className="h-4 w-4 text-mint-400" aria-hidden /> : <Copy className="h-4 w-4" aria-hidden />}
                    </button>
                  </div>
                </div>
                <p className="text-xs text-slate-500">
                  Pay the exact amount <span className="font-bold text-slate-300">{formatINR(intent.amountMinor)}</span> and
                  keep the transaction ID (UTR).
                </p>
              </div>
            </div>

            <div className="glass mt-4 rounded-2xl p-6">
              <label htmlFor="txn" className="block text-sm font-semibold text-white">
                Enter your UPI transaction ID (UTR)
              </label>
              <input
                id="txn"
                value={txnRef}
                onChange={(e) => setTxnRef(e.target.value.toUpperCase())}
                placeholder="e.g. 4231 8930 1122"
                className="mt-2 w-full rounded-xl border border-white/10 bg-ink-800/60 px-4 py-3 font-mono text-sm text-white placeholder:text-slate-600"
              />
              {error && (
                <p role="alert" className="mt-3 rounded-xl border border-red-500/30 bg-red-500/10 px-4 py-3 text-sm text-red-300">
                  {error}
                </p>
              )}
              <button
                onClick={() => void submitClaim()}
                disabled={busy || txnRef.trim().length < 6}
                className="btn-primary mt-4 flex w-full items-center justify-center gap-2 rounded-xl py-3.5 font-semibold text-white disabled:opacity-50"
              >
                {busy ? <Loader2 className="h-5 w-5 animate-spin" aria-hidden /> : <Clock3 className="h-5 w-5" aria-hidden />}
                {busy ? "Submitting…" : "I have paid — submit for verification"}
              </button>
              <p className="mt-3 flex items-center justify-center gap-1.5 text-xs text-slate-500">
                <ShieldCheck className="h-3.5 w-3.5" aria-hidden /> Access is granted after the payment is verified (usually within 30 minutes).
              </p>
            </div>
          </>
        ) : (
          <div className="glass mt-6 rounded-2xl p-8 text-center">
            <BadgeCheck className="mx-auto h-12 w-12 text-mint-400" aria-hidden />
            <h2 className="mt-4 text-xl font-bold text-white">Payment details submitted</h2>
            <p className="mx-auto mt-3 max-w-md text-slate-400">
              We received transaction <span className="font-mono text-slate-200">{txnRef}</span> for order{" "}
              <span className="font-mono text-slate-200">{intent.orderNumber}</span>. Your products unlock as soon as the
              payment is verified — usually within 30 minutes.
            </p>
            <div className="mt-6 flex flex-col items-center gap-3">
              <button onClick={() => { clear(); router.push("/account/orders"); }}
                className="btn-primary rounded-xl px-6 py-3 text-sm font-semibold text-white">
                View my orders
              </button>
              <Link href="/products" className="text-sm text-accent-400 hover:text-white">Continue browsing →</Link>
            </div>
          </div>
        )}
      </div>
    );
  }

  // ---------------- review + gateway selection ----------------
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
        <span className="font-bold text-white">{formatINR(cartTotal)}</span>
      </div>

      {error && (
        <p role="alert" className="mt-4 rounded-xl border border-red-500/30 bg-red-500/10 px-4 py-3 text-sm text-red-300">
          {error}
        </p>
      )}

      <button
        onClick={() => void startPayment()}
        disabled={busy}
        className="btn-primary mt-6 flex w-full items-center justify-center gap-2 rounded-xl py-3.5 font-semibold text-white disabled:opacity-50"
      >
        {busy ? <Loader2 className="h-5 w-5 animate-spin" aria-hidden /> : <CreditCard className="h-5 w-5" aria-hidden />}
        {busy ? "Processing…" : `Proceed to payment · ${formatINR(cartTotal)}`}
      </button>
      <p className="mt-3 flex items-center justify-center gap-1.5 text-xs text-slate-500">
        <ShieldCheck className="h-3.5 w-3.5" aria-hidden /> The final amount is computed and verified server-side.
      </p>
    </div>
  );
}
