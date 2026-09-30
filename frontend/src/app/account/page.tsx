"use client";

import Link from "next/link";
import { useSession } from "@/components/providers/SessionProvider";
import { formatDate } from "@/lib/format";
import { MailWarning } from "lucide-react";

export default function AccountOverview() {
  const { user } = useSession();
  if (!user) return null;

  return (
    <div>
      <h1 className="text-2xl font-bold text-white">Hi, {user.fullName.split(" ")[0]} 👋</h1>
      <p className="mt-1 text-slate-400">Here is your account at a glance.</p>

      {!user.emailVerified && (
        <div className="mt-6 flex items-start gap-3 rounded-2xl border border-gold-500/30 bg-gold-500/10 p-4">
          <MailWarning className="mt-0.5 h-5 w-5 text-gold-400" aria-hidden />
          <div className="flex-1 text-sm">
            <p className="font-semibold text-gold-400">Your email is not verified</p>
            <p className="mt-1 text-slate-300">Check your inbox for the verification link.</p>
            <ResendButton />
          </div>
        </div>
      )}
      {user.mustChangePassword && (
        <div className="mt-4 rounded-2xl border border-gold-500/30 bg-gold-500/10 p-4 text-sm">
          <p className="font-semibold text-gold-400">Please change your temporary password</p>
          <Link href="/account/security" className="mt-2 inline-block text-accent-400 hover:text-white">
            Go to security →
          </Link>
        </div>
      )}

      <dl className="mt-8 grid gap-4 sm:grid-cols-2">
        <div className="glass rounded-2xl p-5">
          <dt className="text-sm text-slate-400">Email</dt>
          <dd className="mt-1 font-semibold text-white">{user.email}</dd>
        </div>
        <div className="glass rounded-2xl p-5">
          <dt className="text-sm text-slate-400">Member since</dt>
          <dd className="mt-1 font-semibold text-white">{formatDate(user.createdAt)}</dd>
        </div>
      </dl>

      <div className="mt-8 grid gap-4 sm:grid-cols-3">
        <Link href="/account/library" className="glass rounded-2xl p-5 transition hover:-translate-y-0.5">
          <p className="text-3xl font-black text-accent-400">Library</p>
          <p className="mt-1 text-sm text-slate-400">Your purchased products</p>
        </Link>
        <Link href="/account/orders" className="glass rounded-2xl p-5 transition hover:-translate-y-0.5">
          <p className="text-3xl font-black text-accent-400">Orders</p>
          <p className="mt-1 text-sm text-slate-400">Purchase history</p>
        </Link>
        <Link href="/account/wishlist" className="glass rounded-2xl p-5 transition hover:-translate-y-0.5">
          <p className="text-3xl font-black text-accent-400">Wishlist</p>
          <p className="mt-1 text-sm text-slate-400">Saved for later</p>
        </Link>
      </div>
    </div>
  );
}

function ResendButton() {
  async function resend() {
    try {
      const { api } = await import("@/lib/api");
      await api.post("/api/v1/auth/resend-verification");
      alert("Verification email sent.");
    } catch {
      alert("Could not send the email right now. Try again shortly.");
    }
  }
  return (
    <button onClick={() => void resend()} className="mt-2 text-accent-400 hover:text-white">
      Resend verification email →
    </button>
  );
}
