"use client";

import { useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { api, ApiError, ensureCsrf } from "@/lib/api";
import { useSession } from "@/components/providers/SessionProvider";
import type { User } from "@/lib/types";

const schema = z.object({
  fullName: z.string().min(2, "Enter your full name").max(120),
  email: z.string().email("Enter a valid email"),
  password: z
    .string()
    .min(10, "At least 10 characters")
    .regex(/[a-z]/, "Needs a lowercase letter")
    .regex(/[A-Z]/, "Needs an uppercase letter")
    .regex(/\d/, "Needs a digit"),
});
type FormData = z.infer<typeof schema>;

export default function RegisterClient() {
  const { register, handleSubmit, formState: { errors, isSubmitting } } = useForm<FormData>({
    resolver: zodResolver(schema),
  });
  const [serverError, setServerError] = useState<string | null>(null);
  const [done, setDone] = useState(false);
  const { refresh } = useSession();
  const router = useRouter();

  async function onSubmit(data: FormData) {
    setServerError(null);
    try {
      await ensureCsrf();
      await api.post<User>("/api/v1/auth/register", data);
      await refresh();
      setDone(true);
    } catch (err) {
      setServerError(err instanceof ApiError ? err.message : "Could not create the account. Please try again.");
    }
  }

  if (done) {
    return (
      <div className="mx-auto max-w-md px-4 py-20 text-center">
        <h1 className="text-2xl font-bold text-white">Account created 🎉</h1>
        <p className="mt-3 text-slate-400">
          Check your inbox for a verification link. Your account works right away — verifying
          keeps it secure.
        </p>
        <button
          onClick={() => router.push("/")}
          className="mt-6 rounded-xl bg-accent-600 px-6 py-3 font-semibold text-white hover:bg-accent-500"
        >
          Start exploring
        </button>
      </div>
    );
  }

  return (
    <div className="mx-auto max-w-md px-4 py-20">
      <h1 className="text-3xl font-bold text-white">Create your account</h1>
      <p className="mt-2 text-slate-400">Join StudyVault and own your learning.</p>

      {serverError && (
        <p role="alert" className="mt-6 rounded-xl border border-red-500/30 bg-red-500/10 px-4 py-3 text-sm text-red-300">
          {serverError}
        </p>
      )}

      <form onSubmit={handleSubmit(onSubmit)} className="glass mt-6 space-y-4 rounded-2xl p-6" noValidate>
        <div>
          <label htmlFor="fullName" className="mb-1 block text-sm text-slate-300">Full name</label>
          <input id="fullName" autoComplete="name" {...register("fullName")}
            className="w-full rounded-xl border border-white/10 bg-ink-800/60 px-4 py-2.5 text-sm text-white" />
          {errors.fullName && <p className="mt-1 text-xs text-red-400">{errors.fullName.message}</p>}
        </div>
        <div>
          <label htmlFor="email" className="mb-1 block text-sm text-slate-300">Email</label>
          <input id="email" type="email" autoComplete="email" {...register("email")}
            className="w-full rounded-xl border border-white/10 bg-ink-800/60 px-4 py-2.5 text-sm text-white" />
          {errors.email && <p className="mt-1 text-xs text-red-400">{errors.email.message}</p>}
        </div>
        <div>
          <label htmlFor="password" className="mb-1 block text-sm text-slate-300">Password</label>
          <input id="password" type="password" autoComplete="new-password" {...register("password")}
            className="w-full rounded-xl border border-white/10 bg-ink-800/60 px-4 py-2.5 text-sm text-white" />
          {errors.password && <p className="mt-1 text-xs text-red-400">{errors.password.message}</p>}
        </div>
        <button
          type="submit" disabled={isSubmitting}
          className="w-full rounded-xl bg-accent-600 py-3 font-semibold text-white transition hover:bg-accent-500 disabled:opacity-50"
        >
          {isSubmitting ? "Creating…" : "Create account"}
        </button>
      </form>

      <p className="mt-4 text-center text-sm text-slate-400">
        Already have an account? <Link href="/login" className="text-accent-400 hover:text-white">Sign in</Link>
      </p>
    </div>
  );
}
