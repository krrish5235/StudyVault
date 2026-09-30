import type { Metadata } from "next";

export const metadata: Metadata = { title: "Privacy policy" };

export default function PrivacyPage() {
  return (
    <article className="mx-auto max-w-3xl px-4 py-14">
      <h1 className="text-3xl font-bold text-white">Privacy policy</h1>
      <div className="prose prose-invert mt-6 max-w-none space-y-4 text-slate-300">
        <p><em>This is the default template - review and customize it before going live.</em></p>
        <h2 className="text-xl font-semibold text-white">What we collect</h2>
        <p>
          StudyVault collects the minimum information needed to operate your account: your email
          address, your name, and records of your purchases and downloads. Payment card details are
          never collected or stored by us - all payment processing is handled by our payment
          provider (Razorpay).
        </p>
        <h2 className="text-xl font-semibold text-white">How we use it</h2>
        <p>
          We use your information to provide access to purchased products, send transactional
          emails (order confirmations, security alerts), and keep the platform secure through
          audit logging. We do not sell personal data.
        </p>
        <h2 className="text-xl font-semibold text-white">Cookies</h2>
        <p>
          We use a single HttpOnly session cookie for authentication and a CSRF protection cookie.
          Both are strictly functional - we do not use advertising or cross-site tracking cookies.
        </p>
        <h2 className="text-xl font-semibold text-white">Your rights</h2>
        <p>
          You can request an export of your data or the deletion of your account at any time by
          contacting us. Deleting your account removes personal data; transaction records may be
          retained where legally required.
        </p>
        <h2 className="text-xl font-semibold text-white">Security</h2>
        <p>
          Passwords are hashed with Argon2id, sessions are server-side and revocable, and paid
          files are delivered only through short-lived signed URLs after purchase verification.
        </p>
      </div>
    </article>
  );
}
