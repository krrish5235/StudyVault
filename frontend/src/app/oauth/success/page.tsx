"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useSession } from "@/components/providers/SessionProvider";
import { Loader2 } from "lucide-react";

/** Landing page after a successful OAuth redirect: establish the client session, then move on. */
export default function OAuthSuccessPage() {
  const { user, loading, refresh } = useSession();
  const router = useRouter();
  const [checked, setChecked] = useState(false);

  useEffect(() => {
    (async () => {
      await refresh();
      setChecked(true);
    })();
  }, [refresh]);

  useEffect(() => {
    if (checked && user) {
      router.replace("/account");
    }
  }, [checked, user, router]);

  if (checked && !user) {
    return (
      <div className="mx-auto max-w-md px-4 py-24 text-center">
        <h1 className="text-2xl font-bold text-white">Sign-in did not complete</h1>
        <p className="mt-3 text-slate-400">We could not establish your session. Please try signing in again.</p>
        <Link href="/login" className="mt-6 inline-block rounded-xl bg-accent-600 px-6 py-3 font-semibold text-white hover:bg-accent-500">
          Back to sign in
        </Link>
      </div>
    );
  }

  return (
    <div className="flex items-center justify-center gap-2 py-32 text-slate-400">
      <Loader2 className="h-5 w-5 animate-spin" aria-hidden /> Finishing sign-in…
    </div>
  );
}
