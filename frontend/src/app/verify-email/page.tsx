"use client";

import { Suspense, useEffect, useState } from "react";
import Link from "next/link";
import { useSearchParams } from "next/navigation";
import { api, ApiError, ensureCsrf } from "@/lib/api";

function VerifyInner() {
  const params = useSearchParams();
  const token = params.get("token");
  const [state, setState] = useState<"loading" | "ok" | "error">("loading");
  const [message, setMessage] = useState("");

  useEffect(() => {
    if (!token) {
      setState("error");
      setMessage("This link is missing its verification token.");
      return;
    }
    (async () => {
      try {
        await ensureCsrf();
        await api.post("/api/v1/auth/verify-email", { token });
        setState("ok");
      } catch (err) {
        setState("error");
        setMessage(err instanceof ApiError ? err.message : "Verification failed.");
      }
    })();
  }, [token]);

  return (
    <div className="text-center">
      {state === "loading" && <p className="text-slate-400">Verifying your email…</p>}
      {state === "ok" && (
        <>
          <h1 className="text-2xl font-bold text-mint-400">Email verified ✓</h1>
          <p className="mt-3 text-slate-400">Your account is fully active. Happy learning!</p>
        </>
      )}
      {state === "error" && (
        <>
          <h1 className="text-2xl font-bold text-red-400">Verification failed</h1>
          <p className="mt-3 text-slate-400">{message}</p>
          <p className="mt-1 text-sm text-slate-500">You can request a new link from your account security page.</p>
        </>
      )}
      <Link href="/" className="mt-6 inline-block rounded-xl bg-accent-600 px-6 py-3 font-semibold text-white hover:bg-accent-500">
        Back to home
      </Link>
    </div>
  );
}

export default function VerifyEmailPage() {
  return (
    <div className="mx-auto max-w-md px-4 py-20">
      <Suspense fallback={<p className="text-center text-slate-400">Loading…</p>}>
        <VerifyInner />
      </Suspense>
    </div>
  );
}
