"use client";

import { useSession } from "@/components/providers/SessionProvider";
import { formatDate } from "@/lib/format";

export default function ProfilePage() {
  const { user } = useSession();
  if (!user) return null;

  return (
    <div>
      <h1 className="text-2xl font-bold text-white">Profile</h1>
      <dl className="glass mt-6 max-w-md space-y-4 rounded-2xl p-6">
        <div>
          <dt className="text-sm text-slate-400">Full name</dt>
          <dd className="mt-0.5 font-semibold text-white">{user.fullName}</dd>
        </div>
        <div>
          <dt className="text-sm text-slate-400">Email</dt>
          <dd className="mt-0.5 font-semibold text-white">
            {user.email}
            {user.emailVerified && <span className="ml-2 text-xs text-mint-400">verified</span>}
          </dd>
        </div>
        <div>
          <dt className="text-sm text-slate-400">Member since</dt>
          <dd className="mt-0.5 font-semibold text-white">{formatDate(user.createdAt)}</dd>
        </div>
      </dl>
      <p className="mt-4 max-w-md text-xs text-slate-500">
        Profile editing, account deletion and data export are available from this page once enabled.
        StudyVault stores only what it needs to operate your account.
      </p>
    </div>
  );
}
