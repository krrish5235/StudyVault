"use client";

import { useEffect } from "react";
import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { useSession } from "@/components/providers/SessionProvider";
import {
  LayoutDashboard, Package, ShoppingBag, Users, Ticket, MessageSquareText,
  ScrollText, Settings, Terminal, Loader2, IndianRupee,
} from "lucide-react";

const nav = [
  { href: "/admin", label: "Dashboard", icon: LayoutDashboard },
  { href: "/admin/payments", label: "Payments", icon: IndianRupee },
  { href: "/admin/products", label: "Products", icon: Package },
  { href: "/admin/orders", label: "Orders", icon: ShoppingBag },
  { href: "/admin/users", label: "Users", icon: Users },
  { href: "/admin/coupons", label: "Coupons", icon: Ticket },
  { href: "/admin/reviews", label: "Reviews", icon: MessageSquareText },
  { href: "/admin/audit", label: "Audit log", icon: ScrollText },
  { href: "/admin/settings", label: "Website", icon: Settings },
  { href: "/admin/command", label: "Command Center", icon: Terminal },
];

/**
 * UX-level gate only - backend RBAC is the real enforcement.
 */
export default function AdminLayout({ children }: { children: React.ReactNode }) {
  const { user, loading } = useSession();
  const router = useRouter();
  const pathname = usePathname();

  useEffect(() => {
    if (!loading && !user) router.replace("/login?next=/admin");
    if (!loading && user && !user.roles.includes("ADMIN")) router.replace("/account");
  }, [loading, user, router]);

  if (loading || !user || !user.roles.includes("ADMIN")) {
    return <div className="flex items-center justify-center py-32 text-slate-400"><Loader2 className="mr-2 h-5 w-5 animate-spin" aria-hidden /> Verifying access…</div>;
  }

  return (
    <div className="mx-auto grid max-w-[1500px] gap-6 px-4 py-8 lg:grid-cols-[230px_1fr]">
      <aside className="h-fit rounded-2xl border border-white/5 p-3 lg:glass">
        <p className="px-3 pb-2 text-xs font-bold uppercase tracking-wider text-slate-500">Command</p>
        <nav aria-label="Admin sections">
          <ul className="flex gap-1 overflow-x-auto lg:flex-col">
            {nav.map((n) => (
              <li key={n.href}>
                <Link
                  href={n.href}
                  className={`flex items-center gap-2 whitespace-nowrap rounded-xl px-3 py-2 text-sm transition ${
                    pathname === n.href ? "bg-accent-600/20 text-white" : "text-slate-400 hover:bg-white/5 hover:text-white"
                  }`}
                >
                  <n.icon className="h-4 w-4" aria-hidden /> {n.label}
                </Link>
              </li>
            ))}
          </ul>
        </nav>
      </aside>
      <div className="min-w-0">{children}</div>
    </div>
  );
}
