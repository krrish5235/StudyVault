import Link from "next/link";
import { ArrowRight, Briefcase, FileDown, ShieldCheck, Star, Target, UserRoundCheck, Zap } from "lucide-react";
import HeroCanvas from "@/components/three/HeroCanvas";
import ProductCard from "@/components/product/ProductCard";
import Tilt from "@/components/ui/Tilt";
import { api } from "@/lib/api";
import type { Category, ProductSummary } from "@/lib/types";

export const revalidate = 60;

async function loadHome(): Promise<{
  featured: ProductSummary[];
  bestsellers: ProductSummary[];
  categories: Category[];
}> {
  try {
    const [featured, bestsellers, categories] = await Promise.all([
      api.get<ProductSummary[]>("/api/v1/products/featured?limit=8"),
      api.get<ProductSummary[]>("/api/v1/products/bestsellers?limit=8"),
      api.get<Category[]>("/api/v1/categories"),
    ]);
    return { featured, bestsellers, categories };
  } catch {
    return { featured: [], bestsellers: [], categories: [] };
  }
}

export default async function HomePage() {
  const { featured, bestsellers, categories } = await loadHome();

  return (
    <div>
      {/* ---------------- Hero ---------------- */}
      <section className="relative overflow-hidden">
        <div className="pointer-events-none absolute inset-0 -z-20" aria-hidden>
          <div className="blob left-[-10%] top-[10%] h-96 w-96 bg-accent-600" />
          <div className="blob right-[-5%] top-[40%] h-80 w-80 bg-gold-500" style={{ animationDelay: "-7s" }} />
        </div>
        <div className="absolute inset-0 -z-10">
          <HeroCanvas />
        </div>
        <div className="mx-auto max-w-7xl px-4 py-28 md:py-40">
          <div className="max-w-3xl">
            <p className="kicker reveal reveal-1 mb-4 inline-flex items-center gap-2 rounded-full border border-white/10 bg-white/5 px-3.5 py-1.5">
              <span className="relative flex h-2 w-2">
                <span className="absolute inline-flex h-full w-full animate-ping rounded-full bg-mint-400 opacity-60" />
                <span className="relative inline-flex h-2 w-2 rounded-full bg-mint-400" />
              </span>
              PLACEMENT SYSTEM · ONLINE
            </p>
            <h1 className="font-display reveal reveal-2 text-5xl font-black leading-[1.05] tracking-tight md:text-7xl">
              <span className="text-gradient glow-accent">Placement-ready.</span>
              <br />
              Interview-proof.
            </h1>
            <p className="reveal reveal-3 mt-6 max-w-xl text-lg text-slate-300">
              Premium notes, a free 150-question FAANG DSA sheet and one-on-one
              interview prep — everything you need to go from learning to placed,
              in one portal.
            </p>
            <div className="reveal reveal-4 mt-9 flex flex-wrap gap-4">
              <Link
                href="/dsa-sheet"
                className="btn-primary inline-flex items-center gap-2 rounded-xl px-7 py-3.5 font-semibold text-white"
              >
                Start the DSA 150 sheet <ArrowRight className="h-4 w-4" aria-hidden />
              </Link>
              <Link
                href="/products"
                className="btn-ghost inline-flex items-center gap-2 rounded-xl px-7 py-3.5 font-semibold text-slate-200"
              >
                <Star className="h-4 w-4 text-gold-400" aria-hidden /> Browse notes
              </Link>
            </div>
          </div>
        </div>
      </section>

      {/* ---------------- Placement pillars ---------------- */}
      <section className="mx-auto max-w-7xl px-4 py-12" aria-label="What you get">
        <p className="kicker">// PLACEMENT SYSTEM</p>
        <h2 className="font-display mt-2 text-3xl font-bold text-white md:text-4xl">Your placement, covered end to end</h2>
        <div className="mt-10 grid gap-5 sm:grid-cols-2 lg:grid-cols-4">
          {[
            {
              href: "/products",
              icon: FileDown,
              accent: "text-accent-400",
              title: "Notes & e-books",
              text: "Deep, structured notes per subject — versioned and updated.",
            },
            {
              href: "/dsa-sheet",
              icon: Target,
              accent: "text-gold-400",
              title: "FAANG DSA 150",
              badge: "FREE",
              text: "Only 150 questions, chosen to cover every FAANG-style interview.",
            },
            {
              href: "/products?category=interview-preparation",
              icon: Briefcase,
              accent: "text-accent-400",
              title: "Interview resources",
              text: "System design, HR rounds, project answers and cheat sheets.",
            },
            {
              href: "/contact",
              icon: UserRoundCheck,
              accent: "text-mint-400",
              title: "1:1 interview prep",
              text: "Mock interviews and a personal plan for your target companies.",
            },
          ].map((p) => (
            <Tilt key={p.href + p.title} max={7}>
              <Link href={p.href} className="glass-edge card-glow block h-full rounded-2xl p-6">
                <p.icon className={`h-7 w-7 ${p.accent}`} aria-hidden />
                <span className="font-display mt-4 flex items-center gap-2 font-semibold text-white">
                  {p.title}
                  {p.badge && (
                    <span className="rounded-full bg-mint-400/10 px-2 py-0.5 text-[10px] font-bold text-mint-400">{p.badge}</span>
                  )}
                </span>
                <p className="mt-2 text-sm text-slate-400">{p.text}</p>
              </Link>
            </Tilt>
          ))}
        </div>
      </section>

      {/* ---------------- Categories ---------------- */}
      {categories.length > 0 && (
        <section className="mx-auto max-w-7xl px-4 py-6" aria-label="Categories">
          <p className="kicker">// CATALOG INDEX</p>
          <div className="mt-4 flex flex-wrap gap-2">
            {categories.slice(0, 14).map((c) => (
              <Link
                key={c.id}
                href={`/products?category=${c.slug}`}
                className="btn-ghost rounded-full px-4 py-1.5 text-sm text-slate-300"
              >
                {c.name}
              </Link>
            ))}
          </div>
        </section>
      )}

      {/* ---------------- Featured ---------------- */}
      {featured.length > 0 && (
        <section className="mx-auto max-w-7xl px-4 py-12">
          <p className="kicker">// HAND-PICKED</p>
          <div className="mb-8 flex items-end justify-between">
            <h2 className="font-display text-3xl font-bold text-white">Featured products</h2>
            <Link href="/products" className="flex items-center gap-1 text-sm text-accent-400 hover:text-accent-300">
              View all <ArrowRight className="h-4 w-4" aria-hidden />
            </Link>
          </div>
          <div className="grid gap-5 sm:grid-cols-2 lg:grid-cols-4">
            {featured.slice(0, 4).map((p) => (
              <ProductCard key={p.id} product={p} />
            ))}
          </div>
        </section>
      )}

      {/* ---------------- Bestsellers ---------------- */}
      {bestsellers.length > 0 && (
        <section className="mx-auto max-w-7xl px-4 py-12 pb-24">
          <p className="kicker">// MOST DOWNLOADED</p>
          <div className="mb-8 flex items-end justify-between">
            <h2 className="font-display text-3xl font-bold text-white">Bestsellers</h2>
            <Link href="/products?sort=popular" className="flex items-center gap-1 text-sm text-accent-400 hover:text-accent-300">
              View all <ArrowRight className="h-4 w-4" aria-hidden />
            </Link>
          </div>
          <div className="grid gap-5 sm:grid-cols-2 lg:grid-cols-4">
            {bestsellers.slice(0, 4).map((p) => (
              <ProductCard key={p.id} product={p} />
            ))}
          </div>
        </section>
      )}

      {featured.length === 0 && bestsellers.length === 0 && (
        <section className="mx-auto max-w-3xl px-4 py-24 text-center">
          <h2 className="font-display text-2xl font-bold text-white">The catalog is warming up</h2>
          <p className="mt-3 text-slate-400">
            Products will appear here as soon as they are published. Check back shortly.
          </p>
        </section>
      )}

      {/* trust strip */}
      <section className="mx-auto max-w-7xl px-4 pb-20" aria-label="Why StudyVault">
        <div className="glass-edge grid gap-6 rounded-2xl p-8 sm:grid-cols-3">
          {[
            { icon: FileDown, title: "Instant delivery", text: "Secure, signed download links the moment your payment clears." },
            { icon: Zap, title: "Written by engineers", text: "Field-tested material distilled from real production experience." },
            { icon: ShieldCheck, title: "Protected & versioned", text: "Original content, verified reviews and free version updates." },
          ].map((v) => (
            <div key={v.title} className="flex gap-4">
              <v.icon className="h-6 w-6 shrink-0 text-accent-400" aria-hidden />
              <div>
                <h3 className="font-display font-semibold text-white">{v.title}</h3>
                <p className="mt-1 text-sm text-slate-400">{v.text}</p>
              </div>
            </div>
          ))}
        </div>
      </section>
    </div>
  );
}
