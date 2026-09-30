import type { Metadata } from "next";

export const metadata: Metadata = { title: "Refund policy" };

export default function RefundPolicyPage() {
  return (
    <article className="mx-auto max-w-3xl px-4 py-14">
      <h1 className="text-3xl font-bold text-white">Refund policy</h1>
      <div className="prose prose-invert mt-6 max-w-none space-y-4 text-slate-300">
        <p><em>This is the default template - review and customize it before going live.</em></p>
        <h2 className="text-xl font-semibold text-white">Digital products</h2>
        <p>
          Because our products are digital files delivered instantly, all sales are generally
          final. However, we want you to be satisfied: if a product is materially different from
          its description, or if you were charged twice, contact us within 7 days of purchase and
          we will make it right - with a replacement or a refund.
        </p>
        <h2 className="text-xl font-semibold text-white">How refunds work</h2>
        <p>
          Approved refunds are returned through the original payment method via our payment
          provider. Refunds revoke access to the refunded product.
        </p>
        <h2 className="text-xl font-semibold text-white">Contact</h2>
        <p>
          Reach us through the <a href="/contact" className="text-accent-400">contact page</a> with
          your order number.
        </p>
      </div>
    </article>
  );
}
