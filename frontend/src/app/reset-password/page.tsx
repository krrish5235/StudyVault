"use client";

import { useState } from "react";
import { Suspense } from "react";
import Link from "next/link";
import { useSearchParams } from "next/navigation";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { api, ApiError, ensureCsrf } from "@/lib/api";

const schema = z.object({
  token: z.string().min(1),
  newPassword: z
    .string()
    .min(10, "At least 10 characters")
    .regex(/[a-z]/, "Needs a lowercase letter")
    .regex(/[A-Z]/, "Needs an uppercase letter")
    .regex(/\d/, "Needs a digit"),
});
type FormData = z.infer<typeof schema>;

function ResetForm() {
  const params = useSearchParams();
  const token = params.get("token") ?? "";
  const { register, handleSubmit, formState: { errors, isSubmitting } } = useForm<FormData>({
    resolver: zodResolver(schema),
    defaultValues: { token, newPassword: "" },
  });
  const [serverError, setServerError] = useState<string | null>(null);
  const [done, setDone] = useState(false);

  async function onSubmit(data: FormData) {
    setServerError(null);
    try {
      await ensureCsrf();
      await api.post("/api/v1/auth/reset-password", data);
      setDone(true);
    } catch (err) {
      setServerError(err instanceof ApiError ? err.message : "Could not reset the password.");
    }
  }

  if (done) {
    return (
      <div className="text-center">
        <h1 className="text-2xl font-bold text-white">Password updated</h1>
        <p className="mt-3 text-slate-400">All devices were signed out. Sign in with your new password.</p>
        <Link href="/login" className="mt-6 inline-block rounded-xl bg-accent-600 px-6 py-3 font-semibold text-white hover:bg-accent-500">
          Sign in
        </Link>
      </div>
    );
  }

  return (
    <form onSubmit={handleSubmit(onSubmit)} className="glass space-y-4 rounded-2xl p-6" noValidate>
      <input type="hidden" {...register("token")} />
      {serverError && (
        <p role="alert" className="rounded-xl border border-red-500/30 bg-red-500/10 px-4 py-3 text-sm text-red-300">
          {serverError}
        </p>
      )}
      <div>
        <label htmlFor="newPassword" className="mb-1 block text-sm text-slate-300">New password</label>
        <input id="newPassword" type="password" autoComplete="new-password" {...register("newPassword")}
          className="w-full rounded-xl border border-white/10 bg-ink-800/60 px-4 py-2.5 text-sm text-white" />
        {errors.newPassword && <p className="mt-1 text-xs text-red-400">{errors.newPassword.message}</p>}
      </div>
      <button type="submit" disabled={isSubmitting}
        className="w-full rounded-xl bg-accent-600 py-3 font-semibold text-white hover:bg-accent-500 disabled:opacity-50">
        {isSubmitting ? "Updating…" : "Set new password"}
      </button>
    </form>
  );
}

export default function ResetPasswordPage() {
  return (
    <div className="mx-auto max-w-md px-4 py-20">
      <h1 className="mb-6 text-3xl font-bold text-white">Choose a new password</h1>
      <Suspense fallback={<p className="text-slate-400">Loading…</p>}>
        <ResetForm />
      </Suspense>
    </div>
  );
}
