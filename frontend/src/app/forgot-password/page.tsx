"use client";

import { useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { api, ApiError, ensureCsrf } from "@/lib/api";

const schema = z.object({ email: z.string().email("Enter a valid email") });
type FormData = z.infer<typeof schema>;

export default function ForgotPasswordPage() {
  const { register, handleSubmit, formState: { errors, isSubmitting } } = useForm<FormData>({
    resolver: zodResolver(schema),
  });
  const [sent, setSent] = useState(false);

  async function onSubmit(data: FormData) {
    try {
      await ensureCsrf();
      await api.post("/api/v1/auth/forgot-password", data);
    } catch (err) {
      if (!(err instanceof ApiError && err.status === 429)) {
        // Generic response either way - never reveal whether the account exists.
      }
    } finally {
      setSent(true);
    }
  }

  return (
    <div className="mx-auto max-w-md px-4 py-20">
      <h1 className="text-3xl font-bold text-white">Reset your password</h1>
      {sent ? (
        <p className="glass mt-6 rounded-2xl p-6 text-slate-300">
          If an account exists for that email, a reset link is on its way. It expires in 30 minutes.
        </p>
      ) : (
        <form onSubmit={handleSubmit(onSubmit)} className="glass mt-6 space-y-4 rounded-2xl p-6" noValidate>
          <div>
            <label htmlFor="email" className="mb-1 block text-sm text-slate-300">Email</label>
            <input id="email" type="email" {...register("email")}
              className="w-full rounded-xl border border-white/10 bg-ink-800/60 px-4 py-2.5 text-sm text-white" />
            {errors.email && <p className="mt-1 text-xs text-red-400">{errors.email.message}</p>}
          </div>
          <button type="submit" disabled={isSubmitting}
            className="w-full rounded-xl bg-accent-600 py-3 font-semibold text-white hover:bg-accent-500 disabled:opacity-50">
            {isSubmitting ? "Sending…" : "Send reset link"}
          </button>
        </form>
      )}
    </div>
  );
}
