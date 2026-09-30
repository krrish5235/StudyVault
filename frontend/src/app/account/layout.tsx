import type { Metadata } from "next";
import Link from "next/link";
import AccountGate from "./AccountGate";
import { BookMarked, Package, Heart, User as UserIcon, ShieldCheck } from "lucide-react";

export const metadata: Metadata = { title: "My account" };

const sections = [
  { href: "/account", label: "Overview", icon: Package },
  { href: "/account/library", label: "My Library", icon: BookMarked },
  { href: "/account/orders", label: "Orders", icon: Package },
  { href: "/account/wishlist", label: "Wishlist", icon: Heart },
  { href: "/account/security", label: "Security", icon: ShieldCheck },
  { href: "/account/profile", label: "Profile", icon: UserIcon },
];

export default function AccountLayout({ children }: { children: React.ReactNode }) {
  return (
    <AccountGate>
      <div className="mx-auto grid max-w-7xl gap-8 px-4 py-10 lg:grid-cols-[240px_1fr]">
        <nav aria-label="Account sections" className="h-fit rounded-2xl border border-white/5 p-3 lg:glass">
          <ul className="flex gap-2 overflow-x-auto lg:flex-col">
            {sections.map((s) => (
              <li key={s.href}>
                <Link
                  href={s.href}
                  className="flex items-center gap-2 whitespace-nowrap rounded-xl px-3 py-2 text-sm text-slate-300 hover:bg-white/5 hover:text-white"
                >
                  <s.icon className="h-4 w-4 text-accent-400" aria-hidden /> {s.label}
                </Link>
              </li>
            ))}
          </ul>
        </nav>
        <div>{children}</div>
      </div>
    </AccountGate>
  );
}
