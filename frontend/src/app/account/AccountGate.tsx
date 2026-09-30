"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";
import { useSession } from "@/components/providers/SessionProvider";

/**
 * UX-level route guard. This only redirects when no session cookie exists;
 * real authorization is enforced by the backend on every API call.
 */
export default function AccountGate({ children }: { children: React.ReactNode }) {
  const { user, loading } = useSession();
  const router = useRouter();

  useEffect(() => {
    if (!loading && !user) {
      router.replace("/login?next=/account");
    }
  }, [loading, user, router]);

  if (loading) {
    return <div className="py-24 text-center text-slate-400">Loading…</div>;
  }
  if (!user) {
    return <div className="py-24 text-center text-slate-400">Redirecting to sign in…</div>;
  }
  return <>{children}</>;
}
