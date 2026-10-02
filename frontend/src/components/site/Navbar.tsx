"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { useState } from "react";
import { ShoppingBag, Menu, X, BookOpenText, LayoutDashboard } from "lucide-react";
import { useSession } from "@/components/providers/SessionProvider";
import { useCart } from "@/components/providers/CartProvider";

const links = [
  { href: "/dsa-sheet", label: "DSA Sheet" },
  { href: "/products", label: "Catalog" },
  { href: "/about", label: "About" },
  { href: "/contact", label: "Contact" },
];

export default function Navbar() {
  const { user, logout, loading } = useSession();
  const { items } = useCart();
  const [open, setOpen] = useState(false);
  const pathname = usePathname();

  const isAdmin = user?.roles.includes("ADMIN");

  return (
    <header className="sticky top-0 z-50 px-4 pt-3">
      <nav
        className="glass-edge mx-auto flex max-w-6xl items-center justify-between rounded-2xl px-5 py-3"
        aria-label="Main"
      >
        <Link href="/" className="font-display flex items-center gap-2 text-lg font-bold tracking-tight">
          <BookOpenText className="h-6 w-6 text-accent-400" aria-hidden />
          <span>
            Study<span className="text-accent-400">Vault</span>
          </span>
        </Link>

        <div className="hidden items-center gap-6 md:flex">
          {links.map((l) => (
            <Link
              key={l.href}
              href={l.href}
              className={`text-sm transition-colors hover:text-white ${
                pathname.startsWith(l.href) ? "text-white" : "text-slate-300"
              }`}
            >
              {l.label}
            </Link>
          ))}
        </div>

        <div className="hidden items-center gap-3 md:flex">
          <Link href="/cart" className="relative rounded-lg p-2 hover:bg-white/5" aria-label={`Cart (${items.length} items)`}>
            <ShoppingBag className="h-5 w-5" aria-hidden />
            {items.length > 0 && (
              <span className="absolute -right-0.5 -top-0.5 flex h-4 w-4 items-center justify-center rounded-full bg-accent-500 text-[10px] font-bold">
                {items.length}
              </span>
            )}
          </Link>
          {!loading && user ? (
            <>
              {isAdmin && (
                <Link href="/admin" className="btn-ghost flex items-center gap-1.5 rounded-lg px-3 py-1.5 text-sm">
                  <LayoutDashboard className="h-4 w-4" aria-hidden /> Admin
                </Link>
              )}
              <Link href="/account" className="btn-primary rounded-lg px-4 py-1.5 text-sm font-semibold text-white">
                {user.fullName.split(" ")[0]}
              </Link>
              <button onClick={() => void logout()} className="text-sm text-slate-300 hover:text-white">
                Sign out
              </button>
            </>
          ) : (
            <>
              <Link href="/login" className="rounded-lg px-3 py-1.5 text-sm text-slate-200 hover:bg-white/5">
                Sign in
              </Link>
              <Link href="/register" className="btn-primary rounded-lg px-4 py-1.5 text-sm font-semibold text-white">
                Create account
              </Link>
            </>
          )}
        </div>

        <button className="rounded-lg p-2 md:hidden" onClick={() => setOpen(!open)} aria-expanded={open} aria-label="Toggle menu">
          {open ? <X className="h-6 w-6" /> : <Menu className="h-6 w-6" />}
        </button>
      </nav>

      {open && (
        <div className="glass-edge mx-auto mt-2 max-w-6xl rounded-2xl px-4 pb-4 md:hidden">
          <div className="flex flex-col gap-3 pt-3">
            {links.map((l) => (
              <Link key={l.href} href={l.href} onClick={() => setOpen(false)} className="text-sm text-slate-200">
                {l.label}
              </Link>
            ))}
            <Link href="/cart" onClick={() => setOpen(false)} className="text-sm text-slate-200">
              Cart ({items.length})
            </Link>
            {user ? (
              <>
                <Link href="/account" onClick={() => setOpen(false)} className="text-sm text-slate-200">
                  My account
                </Link>
                <button onClick={() => void logout()} className="text-left text-sm text-slate-300">
                  Sign out
                </button>
              </>
            ) : (
              <>
                <Link href="/login" onClick={() => setOpen(false)} className="text-sm text-slate-200">
                  Sign in
                </Link>
                <Link href="/register" onClick={() => setOpen(false)} className="text-sm font-semibold text-accent-400">
                  Create account
                </Link>
              </>
            )}
          </div>
        </div>
      )}
    </header>
  );
}
