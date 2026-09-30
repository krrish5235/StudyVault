"use client";

import { useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { api, ApiError, ensureCsrf } from "@/lib/api";
import { useSession } from "@/components/providers/SessionProvider";

const schema = z.object({
  currentPassword: z.string().min(1, "Current password is required"),
  newPassword: z
    .string()
    .min(10, "At least 10 characters")
    .regex(/[a-z]/, "Needs a lowercase letter")
    .regex(/[A-Z]/, "Needs an uppercase letter")
    .regex(/\d/, "Needs a digit"),
});
type FormData = z.infer<typeof schema>;

export default function SecurityPage() {
  const { user } = useSession();
  const { register, handleSubmit, reset, formState: { errors, isSubmitting } } = useForm<FormData>({
    resolver: zodResolver(schema),
  });
  const [message, setMessage] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  async function onSubmit(data: FormData) {
    setMessage(null);
    setError(null);
    try {
      await ensureCsrf();
      const res = await api.post<unknown>("/api/v1/auth/change-password", data);
      reset();
      setMessage(typeof res === "object" ? "Password changed." : "Password changed.");
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Could not change the password.");
    }
  }

  async function signOutEverywhere() {
    try {
      await ensureCsrf();
      await api.post("/api/v1/auth/logout-all");
      window.location.href = "/login";
    } catch {
      setError("Could not sign out other devices.");
    }
  }

  return (
    <div>
      <h1 className="text-2xl font-bold text-white">Security</h1>

      {message && <p className="mt-4 rounded-xl border border-mint-400/30 bg-mint-400/10 px-4 py-3 text-sm text-mint-400">{message}</p>}
      {error && <p role="alert" className="mt-4 rounded-xl border border-red-500/30 bg-red-500/10 px-4 py-3 text-sm text-red-300">{error}</p>}

      <form onSubmit={handleSubmit(onSubmit)} className="glass mt-6 max-w-md space-y-4 rounded-2xl p-6" noValidate>
        <h2 className="font-semibold text-white">Change password</h2>
        <div>
          <label htmlFor="currentPassword" className="mb-1 block text-sm text-slate-300">Current password</label>
          <input id="currentPassword" type="password" autoComplete="current-password" {...register("currentPassword")}
            className="w-full rounded-xl border border-white/10 bg-ink-800/60 px-4 py-2.5 text-sm text-white" />
          {errors.currentPassword && <p className="mt-1 text-xs text-red-400">{errors.currentPassword.message}</p>}
        </div>
        <div>
          <label htmlFor="newPassword" className="mb-1 block text-sm text-slate-300">New password</label>
          <input id="newPassword" type="password" autoComplete="new-password" {...register("newPassword")}
            className="w-full rounded-xl border border-white/10 bg-ink-800/60 px-4 py-2.5 text-sm text-white" />
          {errors.newPassword && <p className="mt-1 text-xs text-red-400">{errors.newPassword.message}</p>}
        </div>
        <button type="submit" disabled={isSubmitting}
          className="rounded-xl bg-accent-600 px-6 py-2.5 font-semibold text-white hover:bg-accent-500 disabled:opacity-50">
          {isSubmitting ? "Updating…" : "Change password"}
        </button>
        <p className="text-xs text-slate-500">Changing your password signs out all other devices.</p>
      </form>

      <div className="glass mt-6 max-w-md rounded-2xl p-6">
        <h2 className="font-semibold text-white">Sign out everywhere</h2>
        <p className="mt-1 text-sm text-slate-400">
          Revokes every active session on every device, including this one.
        </p>
        <button onClick={() => void signOutEverywhere()}
          className="mt-4 rounded-xl border border-red-500/40 px-6 py-2.5 font-semibold text-red-300 hover:bg-red-500/10">
          Sign out all devices
        </button>
      </div>
    </div>
  );
}
