"use client";

import { useState } from "react";
import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { api, ApiError, ensureCsrf } from "@/lib/api";
import { useSession } from "@/components/providers/SessionProvider";
import type { User } from "@/lib/types";

const schema = z.object({
  email: z.string().email("Enter a valid email"),
  password: z.string().min(1, "Password is required"),
});
type FormData = z.infer<typeof schema>;

export default function LoginClient() {
  const { register, handleSubmit, formState: { errors, isSubmitting } } = useForm<FormData>({
    resolver: zodResolver(schema),
  });
  const [serverError, setServerError] = useState<string | null>(null);
  const { refresh } = useSession();
  const router = useRouter();
  const params = useSearchParams();
  const next = params.get("next") ?? "/account";

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

      {serverError && (
        <p role="alert" className="mt-6 rounded-xl border border-red-500/30 bg-red-500/10 px-4 py-3 text-sm text-red-300">
          {serverError}
        </p>
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
