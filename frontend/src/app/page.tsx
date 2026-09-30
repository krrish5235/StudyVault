import Link from "next/link";
import { ArrowRight, ShieldCheck, Zap, FileDown, Star } from "lucide-react";
import HeroCanvas from "@/components/three/HeroCanvas";
import ProductCard from "@/components/product/ProductCard";
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
      {/* Hero */}
      <section className="relative overflow-hidden">
        <div className="absolute inset-0 -z-10">
          <HeroCanvas />
        </div>
        <div className="mx-auto max-w-7xl px-4 py-28 md:py-36">
          <div className="max-w-2xl">
            <p className="mb-4 inline-block rounded-full border border-white/10 bg-white/5 px-3 py-1 text-xs font-medium tracking-wide text-accent-400">
              PREMIUM STUDY MATERIAL · LIFETIME ACCESS
            </p>
            <h1 className="text-4xl font-black leading-tight tracking-tight md:text-6xl">
              <span className="text-gradient">Master the concepts.</span>
              <br />
              Own the interview.
            </h1>
            <p className="mt-6 max-w-xl text-lg text-slate-300">
              Hand-crafted programming notes, e-books and cheat sheets — written by
              engineers, for engineers. Java, Spring Boot, SQL, DSA, System Design and more.
            </p>
            <div className="mt-8 flex flex-wrap gap-4">
              <Link
                href="/products"
                className="inline-flex items-center gap-2 rounded-xl bg-accent-600 px-6 py-3 font-semibold text-white shadow-lg shadow-accent-600/30 transition hover:bg-accent-500"
              >
                Browse the catalog <ArrowRight className="h-4 w-4" aria-hidden />
              </Link>
              <Link
                href="/products?sort=rating"
                className="inline-flex items-center gap-2 rounded-xl border border-white/15 px-6 py-3 font-semibold text-slate-200 transition hover:bg-white/5"
              >
                <Star className="h-4 w-4 text-gold-400" aria-hidden /> Top rated
              </Link>
            </div>
          </div>
        </div>
      </section>

      {/* Value props */}
      <section className="mx-auto max-w-7xl px-4 py-10" aria-label="Why StudyVault">
        <div className="grid gap-4 sm:grid-cols-3">
          {[
            { icon: FileDown, title: "Instant delivery", text: "Secure, signed download links the moment your payment clears." },
            { icon: ShieldCheck, title: "Verified & versioned", text: "Every product is maintained and updated - you get new versions free." },
            { icon: Zap, title: "Written by engineers", text: "Field-tested material distilled from real production experience." },
          ].map((v) => (
            <div key={v.title} className="glass rounded-2xl p-5">
              <v.icon className="h-6 w-6 text-accent-400" aria-hidden />
              <h3 className="mt-3 font-semibold text-white">{v.title}</h3>
              <p className="mt-1 text-sm text-slate-400">{v.text}</p>
            </div>
          ))}
        </div>
      </section>

      {/* Categories */}
      {categories.length > 0 && (
        <section className="mx-auto max-w-7xl px-4 py-8" aria-label="Categories">
          <div className="flex flex-wrap gap-2">
            {categories.slice(0, 12).map((c) => (
              <Link
                key={c.id}
                href={`/products?category=${c.slug}`}
                className="rounded-full border border-white/10 bg-white/5 px-4 py-1.5 text-sm text-slate-300 transition hover:border-accent-400 hover:text-white"
              >
                {c.name}
              </Link>
            ))}
          </div>
        </section>
      )}

      {/* Featured */}
      {featured.length > 0 && (
        <section className="mx-auto max-w-7xl px-4 py-10">
          <div className="mb-6 flex items-end justify-between">
            <h2 className="text-2xl font-bold text-white">Featured products</h2>
            <Link href="/products" className="flex items-center gap-1 text-sm text-accent-400 hover:text-accent-500">
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

      {/* Bestsellers */}
      {bestsellers.length > 0 && (
        <section className="mx-auto max-w-7xl px-4 py-10 pb-20">
          <div className="mb-6 flex items-end justify-between">
            <h2 className="text-2xl font-bold text-white">Bestsellers</h2>
            <Link href="/products?sort=popular" className="flex items-center gap-1 text-sm text-accent-400 hover:text-accent-500">
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
          <h2 className="text-2xl font-bold text-white">The catalog is warming up</h2>
          <p className="mt-3 text-slate-400">
            Products will appear here as soon as they are published. Check back shortly.
          </p>
        </section>
      )}
    </div>
  );
}
