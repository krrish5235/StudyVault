"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { api, API_URL, ApiError, ensureCsrf } from "@/lib/api";
import { useSession } from "@/components/providers/SessionProvider";
import type { User } from "@/lib/types";

const schema = z.object({
  email: z.string().email("Enter a valid email"),
  password: z.string().min(1, "Password is required"),
});
type FormData = z.infer<typeof schema>;

const ERROR_MESSAGES: Record<string, string> = {
  account_disabled: "This account has been disabled. Contact support.",
  no_verified_email: "Your provider account has no verified email address.",
  expired_or_invalid_request: "The sign-in attempt expired. Please try again.",
  oauth_failed: "Sign-in with the provider failed. Please try again or use email sign-in.",
};

export default function LoginClient() {
  const { register, handleSubmit, formState: { errors, isSubmitting } } = useForm<FormData>({
    resolver: zodResolver(schema),
  });
  const [serverError, setServerError] = useState<string | null>(null);
  const [providers, setProviders] = useState<string[]>([]);
  const { refresh } = useSession();
  const router = useRouter();
  const params = useSearchParams();
  const next = params.get("next") ?? "/account";
  const oauthError = params.get("error");

  useEffect(() => {
    api.get<{ providers: string[] }>("/api/v1/auth/oauth/providers")
      .then((r) => setProviders(r.providers ?? []))
      .catch(() => setProviders([]));
  }, []);

  async function onSubmit(data: FormData) {
    setServerError(null);
    try {
      await ensureCsrf();
      await api.post<User>("/api/v1/auth/login", data);
      await refresh();
      router.push(next);
    } catch (err) {
      setServerError(err instanceof ApiError ? err.message : "Could not sign in. Please try again.");
    }
  }

  return (
    <div className="mx-auto max-w-md px-4 py-20">
      <h1 className="text-3xl font-bold text-white">Welcome back</h1>
      <p className="mt-2 text-slate-400">Sign in to access your library.</p>

      {oauthError && ERROR_MESSAGES[oauthError] && (
        <p role="alert" className="mt-6 rounded-xl border border-red-500/30 bg-red-500/10 px-4 py-3 text-sm text-red-300">
          {ERROR_MESSAGES[oauthError]}
        </p>
      )}
      {serverError && (
        <p role="alert" className="mt-6 rounded-xl border border-red-500/30 bg-red-500/10 px-4 py-3 text-sm text-red-300">
          {serverError}
        </p>
      )}

      {providers.length > 0 && (
        <>
          <div className="mt-6 space-y-3">
            {providers.map((p) => (
              <a
                key={p}
                href={`${API_URL}/api/v1/auth/oauth/authorize/${p}`}
                className="flex w-full items-center justify-center gap-3 rounded-xl border border-white/15 py-3 font-semibold text-slate-200 transition hover:bg-white/5"
              >
                {p === "google" ? (
                  <>
                    <svg className="h-5 w-5" viewBox="0 0 24 24" aria-hidden>
                      <path fill="#EA4335" d="M12 5.04c1.7 0 3.22.59 4.42 1.73l3.29-3.29C17.73 1.64 15.09.5 12 .5 7.4.5 3.44 3.13 1.5 7.02l3.84 2.98C6.25 7.14 8.87 5.04 12 5.04z" />
                      <path fill="#4285F4" d="M23.5 12.27c0-.79-.07-1.55-.2-2.27H12v4.51h6.45a5.52 5.52 0 0 1-2.39 3.62l3.71 2.88c2.17-2 3.73-4.96 3.73-8.74z" />
                      <path fill="#FBBC05" d="M5.35 14.5a7.05 7.05 0 0 1 0-4.5L1.5 7.02a11.5 11.5 0 0 0 0 10.46l3.85-2.98z" />
                      <path fill="#34A853" d="M12 23.5c3.09 0 5.69-1.02 7.58-2.77l-3.71-2.88c-1.03.69-2.35 1.1-3.87 1.1-3.13 0-5.75-2.1-6.66-4.95l-3.84 2.98C3.44 20.87 7.4 23.5 12 23.5z" />
                    </svg>
                    Continue with Google
                  </>
                ) : (
                  <svg className="h-5 w-5" viewBox="0 0 16 16" fill="currentColor" aria-hidden>
                    <path d="M8 0C3.58 0 0 3.58 0 8c0 3.54 2.29 6.53 5.47 7.59.4.07.55-.17.55-.38 0-.19-.01-.82-.01-1.49-2.01.37-2.53-.49-2.69-.94-.09-.23-.48-.94-.82-1.13-.28-.15-.68-.52-.01-.53.63-.01 1.08.58 1.23.82.72 1.21 1.87.87 2.33.66.07-.52.28-.87.51-1.07-1.78-.2-3.64-.89-3.64-3.95 0-.87.31-1.59.82-2.15-.08-.2-.36-1.02.08-2.12 0 0 .67-.21 2.2.82a7.6 7.6 0 0 1 2-.27c.68 0 1.36.09 2 .27 1.53-1.04 2.2-.82 2.2-.82.44 1.1.16 1.92.08 2.12.51.56.82 1.27.82 2.15 0 3.07-1.87 3.75-3.65 3.95.29.25.54.73.54 1.48 0 1.07-.01 1.93-.01 2.2 0 .21.15.46.55.38A8.01 8.01 0 0 0 16 8c0-4.42-3.58-8-8-8z" />
                  </svg>
                )}
                Continue with {p === "google" ? "Google" : "GitHub"}
              </a>
            ))}
            <div className="flex items-center gap-3 text-xs text-slate-500">
              <span className="h-px flex-1 bg-white/10" /> or continue with email <span className="h-px flex-1 bg-white/10" />
            </div>
          </div>
        </>
      )}

      <form onSubmit={handleSubmit(onSubmit)} className="glass mt-6 space-y-4 rounded-2xl p-6" noValidate>
        <div>
          <label htmlFor="email" className="mb-1 block text-sm text-slate-300">Email</label>
          <input
            id="email" type="email" autoComplete="email" {...register("email")}
            className="w-full rounded-xl border border-white/10 bg-ink-800/60 px-4 py-2.5 text-sm text-white"
          />
          {errors.email && <p className="mt-1 text-xs text-red-400">{errors.email.message}</p>}
        </div>
        <div>
          <label htmlFor="password" className="mb-1 block text-sm text-slate-300">Password</label>
          <input
            id="password" type="password" autoComplete="current-password" {...register("password")}
            className="w-full rounded-xl border border-white/10 bg-ink-800/60 px-4 py-2.5 text-sm text-white"
          />
          {errors.password && <p className="mt-1 text-xs text-red-400">{errors.password.message}</p>}
        </div>
        <button
          type="submit" disabled={isSubmitting}
          className="w-full rounded-xl bg-accent-600 py-3 font-semibold text-white transition hover:bg-accent-500 disabled:opacity-50"
        >
          {isSubmitting ? "Signing in…" : "Sign in"}
        </button>
      </form>

      <div className="mt-4 flex justify-between text-sm text-slate-400">
        <Link href="/forgot-password" className="hover:text-white">Forgot password?</Link>
        <Link href="/register" className="hover:text-white">Create account</Link>
      </div>
    </div>
  );
}
