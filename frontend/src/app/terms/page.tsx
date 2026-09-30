import type { Metadata } from "next";

export const metadata: Metadata = { title: "Terms & conditions" };

export default function TermsPage() {
  return (
    <article className="mx-auto max-w-3xl px-4 py-14">
      <h1 className="text-3xl font-bold text-white">Terms &amp; conditions</h1>
      <div className="prose prose-invert mt-6 max-w-none space-y-4 text-slate-300">
        <p><em>This is the default template - review and customize it before going live.</em></p>
        <h2 className="text-xl font-semibold text-white">Purchases</h2>
        <p>
          Products sold on StudyVault are digital. After a successful payment you receive
          lifetime access to the purchased product through your library, including future
          content updates.
        </p>
        <h2 className="text-xl font-semibold text-white">License</h2>
        <p>
          Each purchase grants a non-exclusive, non-transferable license for personal use by the
          purchasing account. Redistribution, reselling, or public sharing of purchased files is
          prohibited. Accounts engaging in piracy lose access without refund.
        </p>
        <h2 className="text-xl font-semibold text-white">Accounts</h2>
        <p>
          You are responsible for keeping your credentials secure. We may suspend accounts engaged
          in fraud, abuse, or payment chargeback abuse.
        </p>
        <h2 className="text-xl font-semibold text-white">Liability</h2>
        <p>
          Study material is provided for educational purposes. While we maintain high quality
          standards, StudyVault is not liable for exam outcomes or employment results.
        </p>
      </div>
    </article>
  );
}
