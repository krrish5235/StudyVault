import Link from "next/link";
import { BookOpenText, Code2, ShieldCheck } from "lucide-react";

export default function Footer() {
  return (
    <footer className="relative mt-10 border-t border-white/5 py-12">
      <div className="mx-auto grid max-w-7xl gap-8 px-4 sm:grid-cols-2 lg:grid-cols-4">
        <div>
          <div className="flex items-center gap-2 font-bold">
            <BookOpenText className="h-5 w-5 text-accent-400" aria-hidden />
            Study<span className="text-accent-400">Vault</span>
          </div>
          <p className="mt-3 max-w-xs text-sm text-slate-400">
            Built for engineers who take their craft seriously.
          </p>
          <p className="mt-3 flex items-center gap-1.5 text-xs text-slate-500">
            <ShieldCheck className="h-3.5 w-3.5 text-mint-400" aria-hidden />
            All product content is protected — © {new Date().getFullYear()} StudyVault.
          </p>
        </div>
        <nav aria-label="Shop">
          <h3 className="text-sm font-semibold text-slate-200">Shop</h3>
          <ul className="mt-3 space-y-2 text-sm text-slate-400">
            <li><Link href="/products" className="hover:text-white">All products</Link></li>
            <li><Link href="/products?sort=popular" className="hover:text-white">Bestsellers</Link></li>
            <li><Link href="/products?sort=rating" className="hover:text-white">Top rated</Link></li>
          </ul>
        </nav>
        <nav aria-label="Company">
          <h3 className="text-sm font-semibold text-slate-200">Company</h3>
          <ul className="mt-3 space-y-2 text-sm text-slate-400">
            <li><Link href="/about" className="hover:text-white">About</Link></li>
            <li><Link href="/contact" className="hover:text-white">Contact</Link></li>
          </ul>
        </nav>
        <nav aria-label="Legal">
          <h3 className="text-sm font-semibold text-slate-200">Legal</h3>
          <ul className="mt-3 space-y-2 text-sm text-slate-400">
            <li><Link href="/privacy" className="hover:text-white">Privacy policy</Link></li>
            <li><Link href="/terms" className="hover:text-white">Terms &amp; conditions</Link></li>
            <li><Link href="/refund-policy" className="hover:text-white">Refund policy</Link></li>
          </ul>
        </nav>
      </div>

      <div className="mx-auto mt-10 flex max-w-7xl flex-col items-center justify-between gap-3 border-t border-white/5 px-4 pt-6 text-xs text-slate-500 sm:flex-row">
        <p>© {new Date().getFullYear()} StudyVault. All rights reserved. Unauthorized copying or redistribution of product content is prohibited.</p>
        <p className="flex items-center gap-1.5">
          Designed &amp; developed by
          <span className="flex items-center gap-1 font-semibold text-slate-300">
            <Code2 className="h-3.5 w-3.5 text-accent-400" aria-hidden />
            Krrish
          </span>
        </p>
      </div>
    </footer>
  );
}
