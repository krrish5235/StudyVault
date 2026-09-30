import type { Metadata } from "next";
import { Mail } from "lucide-react";

export const metadata: Metadata = { title: "Contact" };

export default function ContactPage() {
  return (
    <div className="mx-auto max-w-3xl px-4 py-14">
      <h1 className="text-3xl font-bold text-white">Contact</h1>
      <p className="mt-6 text-slate-300">
        Questions about a product, an order, or refunds? Reach out and you will hear back soon.
      </p>
      <div className="glass mt-8 flex items-center gap-4 rounded-2xl p-6">
        <Mail className="h-6 w-6 text-accent-400" aria-hidden />
        <div>
          <p className="text-sm text-slate-400">Email</p>
          <p className="font-semibold text-white">support@studyvault.local</p>
        </div>
      </div>
      <p className="mt-6 text-sm text-slate-500">
        Please include your order number for purchase-related questions.
      </p>
    </div>
  );
}
